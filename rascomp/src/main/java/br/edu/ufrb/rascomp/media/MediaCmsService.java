package br.edu.ufrb.rascomp.media;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import br.edu.ufrb.rascomp.storage.R2ManagedFileStorage;
import lombok.RequiredArgsConstructor;

/** Explicitly staged editorial changes. Public readers only see committed snapshots. */
@Service
@RequiredArgsConstructor
public class MediaCmsService {
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_EDITORIAL_CHARS = 1_000_000;
    private static final Pattern MANAGED_IMAGE = Pattern.compile("/api/v1/(?:public/)?media/assets/(\\d+)/content");
    private final JdbcTemplate db;
    private final ObjectMapper mapper;
    private final ObjectProvider<R2ManagedFileStorage> storage;

    private R2ManagedFileStorage storage() {
        R2ManagedFileStorage current = storage.getIfAvailable();
        if(current == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Armazenamento R2 indisponível");
        return current;
    }
    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);
    }
    private boolean exists(String table,long id) {
        if(!table.equals("media_asset")&&!table.equals("media_album"))throw bad("Tipo inválido");
        Integer count=db.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE id=?",Integer.class,id);
        return count!=null&&count>0;
    }
    private void require(String table,long id) {
        if(!exists(table,id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Registro de mídia não encontrado");
    }
    private JsonNode parse(String value,JsonNode fallback) {
        try{return value==null?fallback:mapper.readTree(value);}
        catch(JsonProcessingException e){throw new IllegalStateException("JSON editorial armazenado inválido",e);}
    }
    private String json(JsonNode data) {
        try{
            String encoded=mapper.writeValueAsString(data);
            if(encoded.length()>MAX_EDITORIAL_CHARS)throw bad("Rascunho excede o limite");
            return encoded;
        }catch(JsonProcessingException e){throw bad("JSON inválido");}
    }
    private JsonNode emptyArray(){return mapper.createArrayNode();}
    private JsonNode emptyObject(){return mapper.createObjectNode();}
    private String state(String column) {
        if(!Set.of("draft_editorial","published_editorial","draft_featured","published_gallery").contains(column))throw bad("Campo inválido");
        return db.queryForObject("SELECT "+column+" FROM media_site_state WHERE id=1",String.class);
    }
    private long version(){return db.queryForObject("SELECT version FROM media_site_state WHERE id=1",Long.class);}

    public List<Map<String,Object>> assets(){
        return db.query("SELECT id,file_name,content_type,bytes_size,gallery_draft FROM media_asset ORDER BY id DESC",
            (rs,n)->{
                long id=rs.getLong("id");
                Map<String,Object> item=new LinkedHashMap<>();
                item.put("id",id);
                item.put("name",rs.getString("file_name"));
                item.put("contentType",rs.getString("content_type"));
                item.put("size",rs.getLong("bytes_size"));
                item.put("published",rs.getBoolean("gallery_draft"));
                item.put("src","/api/v1/media/assets/"+id+"/content");
                item.put("albums",db.queryForList("SELECT album_id FROM media_album_asset WHERE asset_id=? ORDER BY album_id",Long.class,id));
                return item;
            });
    }
    public List<Map<String,Object>> albums(){
        return db.query("SELECT id,name,description,cover_asset_id FROM media_album ORDER BY id",(rs,n)->{
            Map<String,Object> item=new LinkedHashMap<>();
            item.put("id",rs.getLong("id"));item.put("name",rs.getString("name"));
            item.put("description",rs.getString("description"));
            item.put("coverId",rs.getObject("cover_asset_id",Long.class));
            return item;
        });
    }
    public Map<String,Object> workspace() {
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("photos",assets());
        out.put("albums",albums());
        out.put("editorialDraft",parse(state("draft_editorial"),emptyObject()));
        out.put("featuredPhotoIds",parse(state("draft_featured"),emptyArray()));
        out.put("version",version());
        return out;
    }
    private String validContentType(byte[] bytes) {
        if(bytes.length>=8 && bytes[0]==(byte)0x89 && bytes[1]==0x50 && bytes[2]==0x4e && bytes[3]==0x47) return "image/png";
        if(bytes.length>=3 && bytes[0]==(byte)0xff && bytes[1]==(byte)0xd8 && bytes[2]==(byte)0xff) return "image/jpeg";
        if(bytes.length>=12 && new String(bytes,0,4,StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes,8,4,StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
        if(bytes.length>=6 && new String(bytes,0,3,StandardCharsets.US_ASCII).equals("GIF"))return "image/gif";
        throw bad("Formato de imagem não suportado: use PNG, JPEG, GIF ou WebP");
    }
    @Transactional
    public Map<String,Object> upload(MultipartFile file, boolean galleryDraft, String author) {
        if(file.isEmpty()||file.getSize()>MAX_IMAGE_BYTES)throw bad("Imagem vazia ou acima de 10 MB");
        byte[] bytes;
        try{bytes=file.getBytes();}catch(IOException e){throw bad("Não foi possível ler a imagem");}
        if(bytes.length==0||bytes.length>MAX_IMAGE_BYTES)throw bad("Imagem inválida");
        String contentType=validContentType(bytes);
        String rawName=file.getOriginalFilename()==null?"imagem":file.getOriginalFilename();
        String name=rawName.replaceAll("[\\p{Cntrl}/\\\\]","_");
        if(name.isBlank())name="imagem";
        if(name.length()>255)name=name.substring(0,255);
        String key="gallery/"+UUID.randomUUID();
        storage().upload(key,bytes,contentType);
        KeyHolder generated=new GeneratedKeyHolder();
        String safeName=name;
        db.update(con->{
            var statement=con.prepareStatement("INSERT INTO media_asset(file_name,content_type,bytes_size,object_key,gallery_draft,created_by) VALUES (?,?,?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setString(1,safeName);statement.setString(2,contentType);
            statement.setLong(3,bytes.length);statement.setString(4,key);
            statement.setBoolean(5,galleryDraft);statement.setString(6,author);
            return statement;
        },generated);
        Number id=(Number)generated.getKey();
        if(id==null)throw new IllegalStateException("ID de mídia não gerado");
        return Map.of("id",id.longValue(),"name",name,"src","/api/v1/media/assets/"+id.longValue()+"/content",
            "published",galleryDraft,"albums",List.of(),"size",bytes.length,"contentType",contentType);
    }
    public org.springframework.core.io.Resource content(long id,boolean publicly) {
        require("media_asset",id);
        if(publicly){
            Integer count=db.queryForObject("SELECT COUNT(*) FROM media_published_asset WHERE asset_id=?",Integer.class,id);
            if(count==null||count==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Mídia não publicada");
        }
        String key=db.queryForObject("SELECT object_key FROM media_asset WHERE id=?",String.class,id);
        return storage().read(key);
    }
    public String mime(long id) {
        require("media_asset",id);
        return db.queryForObject("SELECT content_type FROM media_asset WHERE id=?",String.class,id);
    }
    @Transactional
    public void visibility(long id,boolean visible) {
        require("media_asset",id);
        db.update("UPDATE media_asset SET gallery_draft=? WHERE id=?",visible,id);
    }
    @Transactional
    public void setAlbums(long id,List<Long> albumIds,boolean append) {
        require("media_asset",id);
        if(albumIds==null || albumIds.size()>100)throw bad("Associação de álbuns inválida");
        if(!append)db.update("DELETE FROM media_album_asset WHERE asset_id=?",id);
        for(Long albumId:new HashSet<>(albumIds)){
            if(albumId==null)throw bad("Álbum inválido");
            require("media_album",albumId);
            db.update("INSERT INTO media_album_asset (album_id,asset_id) SELECT ?,? WHERE NOT EXISTS (SELECT 1 FROM media_album_asset WHERE album_id=? AND asset_id=?)",
                albumId,id,albumId,id);
        }
    }
    @Transactional
    public void batchAlbums(List<Long> photoIds,List<Long> albumIds){
        if(photoIds==null||photoIds.size()>200||albumIds==null||albumIds.size()>100)throw bad("Seleção muito grande");
        for(Long id:new HashSet<>(photoIds)){if(id==null)throw bad("Imagem inválida");setAlbums(id,albumIds,true);}
    }
    @Transactional
    public long createAlbum(String name,String description){
        String title=albumName(name);
        KeyHolder key=new GeneratedKeyHolder();
        db.update(con->{
            var st=con.prepareStatement("INSERT INTO media_album(name,description) VALUES (?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);
            st.setString(1,title);st.setString(2,description==null?"":description.substring(0,Math.min(1000,description.length())));
            return st;
        },key);
        return key.getKey().longValue();
    }
    private String albumName(String name) {
        if(name==null||name.isBlank()||name.trim().length()>160)throw bad("Nome do álbum inválido");
        return name.trim();
    }
    @Transactional
    public void updateAlbum(long id,String name,String description,Long coverId){
        require("media_album",id);
        if(coverId!=null){
            require("media_asset",coverId);
            Integer exists=db.queryForObject("SELECT COUNT(*) FROM media_album_asset WHERE album_id=? AND asset_id=?",Integer.class,id,coverId);
            if(exists==null||exists==0)throw bad("Capa não pertence ao álbum");
        }
        String desc=description==null?"":description;
        if(desc.length()>1000)throw bad("Descrição muito extensa");
        db.update("UPDATE media_album SET name=?,description=?,cover_asset_id=? WHERE id=?",albumName(name),desc,coverId,id);
    }
    @Transactional
    public void deleteAlbum(long id){
        require("media_album",id);
        db.update("DELETE FROM media_album WHERE id=?",id);
    }
    @Transactional
    public void deleteAsset(long id) {
        require("media_asset",id);
        Integer published=db.queryForObject("SELECT COUNT(*) FROM media_published_asset WHERE asset_id=?",Integer.class,id);
        if(published!=null&&published>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Despublique antes de excluir a imagem");
        // Never remove the row/metadata before remote deletion succeeds.
        String key=db.queryForObject("SELECT object_key FROM media_asset WHERE id=?",String.class,id);
        storage().delete(key);
        db.update("UPDATE media_album SET cover_asset_id=NULL WHERE cover_asset_id=?",id);
        db.update("DELETE FROM media_asset WHERE id=?",id);
    }
    @Transactional
    public void saveDraft(JsonNode payload) {
        if(payload==null||!payload.isObject())throw bad("Rascunho deve ser um objeto JSON");
        db.update("UPDATE media_site_state SET draft_editorial=? WHERE id=1",json(payload));
    }
    @Transactional
    public void saveFeatured(JsonNode payload) {
        if(payload==null||!payload.isArray()||payload.size()>3)throw bad("Selecione no máximo 3 imagens");
        Set<Long> unique=new HashSet<>();
        for(JsonNode node:payload){
            if(!node.canConvertToLong())throw bad("ID inválido");
            long id=node.longValue();
            require("media_asset",id);
            if(!unique.add(id))throw bad("Destaques duplicados");
        }
        db.update("UPDATE media_site_state SET draft_featured=? WHERE id=1",json(payload));
    }
    private List<Long> referencedAssets(JsonNode root) {
        List<Long> ids=new ArrayList<>();
        walkReferences(root,ids);
        return ids;
    }
    private void walkReferences(JsonNode node,List<Long> ids) {
        if(node==null||node.isNull())return;
        if(node.isArray()){node.forEach(child->walkReferences(child,ids));return;}
        if(!node.isObject())return;
        node.properties().forEach(entry->{
            if(entry.getKey().equals("src")&&entry.getValue().isTextual()){
                String value=entry.getValue().asText();
                Matcher matcher=MANAGED_IMAGE.matcher(value);
                if(matcher.find())ids.add(Long.parseLong(matcher.group(1)));
                else if(value.startsWith("blob:")||value.contains("localhost")||value.startsWith("data:"))
                    throw bad("Rascunho possui imagem local: envie-a ao R2 antes de publicar");
            }else walkReferences(entry.getValue(),ids);
        });
    }
    private void toPublicSources(JsonNode node) {
        if(node.isArray()){node.forEach(this::toPublicSources);return;}
        if(!node.isObject())return;
        ObjectNode obj=(ObjectNode)node;
        obj.properties().forEach(entry->{
            if(entry.getKey().equals("src")&&entry.getValue().isTextual()){
                String text=entry.getValue().asText();
                Matcher matcher=MANAGED_IMAGE.matcher(text);
                if(matcher.find())obj.put("src","/api/v1/public/media/assets/"+matcher.group(1)+"/content");
            }else toPublicSources(entry.getValue());
        });
    }
    @Transactional
    public long publish(Long expectedVersion) {
        return publishInternal(expectedVersion,true);
    }
    /** Publica apenas fotos, álbuns e destaques, sem aprovar mudanças editoriais pendentes. */
    @Transactional
    public long publishGallery(Long expectedVersion) {
        return publishInternal(expectedVersion,false);
    }
    private long publishInternal(Long expectedVersion,boolean includeEditorialDraft) {
        Long before=db.queryForObject("SELECT version FROM media_site_state WHERE id=1 FOR UPDATE",Long.class);
        if(expectedVersion!=null && !before.equals(expectedVersion))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Outra publicação ocorreu. Atualize o rascunho");
        JsonNode editorial=parse(state(includeEditorialDraft ? "draft_editorial" : "published_editorial"),emptyObject()).deepCopy();
        // Dados de interface e aliases internos nunca podem sair no snapshot público.
        if(editorial.isObject())((ObjectNode)editorial).remove("_editorState");
        if(!editorial.isObject())throw bad("Rascunho inválido");
        Set<Long> approved=new HashSet<>();
        for(Long id:referencedAssets(editorial)){require("media_asset",id);approved.add(id);}
        toPublicSources(editorial);
        ArrayNode gallery=mapper.createArrayNode();
        var publicRows=db.queryForList("SELECT id,file_name FROM media_asset WHERE gallery_draft=TRUE ORDER BY id DESC");
        var publicIds=new HashSet<Long>();
        for(var row:publicRows) {
            long id=((Number)row.get("id")).longValue();
            publicIds.add(id);approved.add(id);
            ObjectNode image=mapper.createObjectNode();
            image.put("id",id);image.put("alt","Fotografia da IEEE RAS UFRB");image.put("published",true);
            image.put("src","/api/v1/public/media/assets/"+id+"/content");
            ArrayNode linked=image.putArray("albumIds");
            for(Long albumId:db.queryForList("SELECT album_id FROM media_album_asset WHERE asset_id=?",Long.class,id))
                linked.add(albumId);
            gallery.add(image);
        }
        ArrayNode featured=mapper.createArrayNode();
        JsonNode draftFeatured=parse(state("draft_featured"),emptyArray());
        for(JsonNode id:draftFeatured){
            if(id.canConvertToLong() && publicIds.contains(id.longValue())&&featured.size()<3)featured.add(id.longValue());
        }
        ObjectNode snapshot=mapper.createObjectNode();
        snapshot.set("photos",gallery);
        ArrayNode albumData=snapshot.putArray("albums");
        for(Map<String,Object> album:albums()){
            long id=((Number)album.get("id")).longValue();
            boolean any=publicRows.stream().anyMatch(row->{
                long photoId=((Number)row.get("id")).longValue();
                Integer count=db.queryForObject("SELECT COUNT(*) FROM media_album_asset WHERE album_id=? AND asset_id=?",Integer.class,id,photoId);
                return count!=null&&count>0;
            });
            if(!any)continue;
            ObjectNode a=albumData.addObject();
            a.put("id",id);a.put("name",(String)album.get("name"));a.put("description",(String)album.get("description"));
            if(album.get("coverId") instanceof Number c && publicIds.contains(c.longValue()))a.put("coverId",c.longValue());
        }
        snapshot.set("featuredPhotoIds",featured);
        db.update("DELETE FROM media_published_asset");
        for(Long id:approved)db.update("INSERT INTO media_published_asset(asset_id) VALUES (?)",id);
        if(includeEditorialDraft){
            db.update("UPDATE media_site_state SET published_editorial=?,published_gallery=?,version=version+1,published_at=CURRENT_TIMESTAMP WHERE id=1",
                json(editorial),json(snapshot));
        }else{
            // Manter o conteúdo editorial publicado inalterado quando o usuário publica só a Galeria.
            db.update("UPDATE media_site_state SET published_gallery=?,version=version+1,published_at=CURRENT_TIMESTAMP WHERE id=1",
                json(snapshot));
        }
        return before+1;
    }
    public JsonNode publishedEditorial() {
        return parse(state("published_editorial"),emptyObject());
    }
    public JsonNode publishedGallery() {
        ObjectNode empty=mapper.createObjectNode();
        empty.set("photos",emptyArray());
        empty.set("albums",emptyArray());
        empty.set("featuredPhotoIds",emptyArray());
        return parse(state("published_gallery"),empty);
    }
}
