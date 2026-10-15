package br.edu.ufrb.rascomp.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import br.edu.ufrb.rascomp.storage.R2ManagedFileStorage;

/** Documents are private by default; the participant API never exposes drafts. */
@RestController
@RequestMapping("/api/v1/ajuda")
public class HelpDocumentController {
 private final JdbcTemplate db;
 private final ObjectProvider<R2ManagedFileStorage> storage;
 private final Path localRoot;
 public HelpDocumentController(JdbcTemplate db,ObjectProvider<R2ManagedFileStorage> storage,
   @Value("${app.storage.help-documents-dir:./uploads/help-documents}") String directory) {
  this.db=db;this.storage=storage;this.localRoot=Paths.get(directory).toAbsolutePath().normalize();
 }
 private static final String FIELDS="id,type,title,description,scope_competition_id AS competitionId,filename,version,published,created_at AS createdAt,updated_at AS updatedAt";
 @GetMapping
 @PreAuthorize("hasAnyRole('PARTICIPANTE','DEV')")
 public List<Map<String,Object>> published(@RequestParam(required=false) Long competitionId) {
  return db.queryForList("SELECT "+FIELDS+" FROM help_document WHERE published=true AND (scope_competition_id IS NULL OR scope_competition_id=?) ORDER BY type,title,id",competitionId==null?-1L:competitionId);
 }
 @GetMapping("/admin")
 @PreAuthorize("hasRole('DEV')")
 public List<Map<String,Object>> admin() {
  return db.queryForList("SELECT "+FIELDS+" FROM help_document ORDER BY id DESC");
 }
 @PostMapping(value="/admin",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 @PreAuthorize("hasRole('DEV')")
 public Map<String,Long> upload(@RequestParam String type,@RequestParam String title,
   @RequestParam(required=false,defaultValue="") String description,
   @RequestParam(required=false) Long competitionId,@RequestParam MultipartFile file,Authentication auth) throws IOException {
  if(!List.of("COMPETICAO","CONDUTA","MANUAL").contains(type)) throw bad("Tipo inválido");
  if(title.isBlank()||title.length()>180||description.length()>1000)throw bad("Título ou descrição inválidos");
  byte[] bytes=file.getBytes();
  if(bytes.length<5||bytes.length>10*1024*1024||bytes[0]!='%'||bytes[1]!='P'||bytes[2]!='D'||bytes[3]!='F'||bytes[4]!='-')throw bad("Envie um PDF válido de até 10 MB");
  String key="competition/help/"+UUID.randomUUID()+".pdf";
  R2ManagedFileStorage r2=storage.getIfAvailable();
  if(r2!=null)r2.upload(key,bytes,"application/pdf");
  else {Path dest=localRoot.resolve(key).normalize();Files.createDirectories(dest.getParent());Files.write(dest,bytes);}
  String safeName=file.getOriginalFilename()==null?"documento.pdf":file.getOriginalFilename().replace('\\','/').replaceAll("^.*/","").replaceAll("[\\r\\n\\\"]","_");
  db.update("INSERT INTO help_document (type,title,description,scope_competition_id,filename,storage_key,created_by) VALUES (?,?,?,?,?,?,?)",
   type,title.trim(),description.trim(),competitionId,safeName,(r2==null?"local:":"r2:")+key,auth.getName());
  return Map.of("id",db.queryForObject("SELECT LAST_INSERT_ID()",Long.class));
 }
 @PatchMapping("/admin/{id}/publication")
 @PreAuthorize("hasRole('DEV')")
 public void publish(@PathVariable long id,@RequestBody Publication request) {
  if(db.update("UPDATE help_document SET published=? WHERE id=?",request.published(),id)==0)
   throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Documento não encontrado");
 }
 public record Publication(boolean published){}
 @GetMapping("/{id}/file")
 @PreAuthorize("hasAnyRole('PARTICIPANTE','DEV')")
 public ResponseEntity<Resource> file(@PathVariable long id,Authentication auth) throws IOException {
  List<Map<String,Object>> rows=db.queryForList("SELECT storage_key,filename,published FROM help_document WHERE id=?",id);
  if(rows.isEmpty()||(!Boolean.TRUE.equals(rows.get(0).get("published"))&&!auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_DEV"))))
   throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Documento indisponível");
  String key=(String)rows.get(0).get("storage_key");
  Resource body;
  if(key.startsWith("r2:")) {
   R2ManagedFileStorage r2=storage.getIfAvailable();
   if(r2==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Armazenamento indisponível");
   body=r2.read(key.substring(3));
  } else {
   Path path=localRoot.resolve(key.substring(6)).normalize();
   if(!path.startsWith(localRoot))throw bad("Arquivo inválido");
   body=new UrlResource(path.toUri());
  }
  if(!body.exists())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Arquivo indisponível");
  return ResponseEntity.ok().cacheControl(CacheControl.noStore())
   .header("X-Content-Type-Options","nosniff")
   .header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\"documento-"+id+".pdf\"")
   .contentType(MediaType.APPLICATION_PDF).body(body);
 }
 private static ResponseStatusException bad(String msg){return new ResponseStatusException(HttpStatus.BAD_REQUEST,msg);}
}
