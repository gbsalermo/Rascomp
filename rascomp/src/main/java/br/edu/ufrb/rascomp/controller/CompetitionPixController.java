package br.edu.ufrb.rascomp.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class CompetitionPixController {
 private final JdbcTemplate db;
 public CompetitionPixController(JdbcTemplate db) { this.db=db; }
 public record PixPayment(String holderName,String pixKey) {}
 @GetMapping("/participante/pix/{competitionId}")
 @PreAuthorize("hasAnyRole('PARTICIPANTE','DEV')")
 public PixPayment participant(@PathVariable long competitionId) { return find(competitionId); }
 @GetMapping("/pix/admin/{competitionId}")
 @PreAuthorize("hasRole('DEV')")
 public PixPayment admin(@PathVariable long competitionId) { return find(competitionId); }
 private PixPayment find(long id) {
  List<PixPayment> rows=db.query("SELECT holder_name,pix_key FROM competition_pix_payment WHERE competition_id=?",
   (rs,n)->new PixPayment(rs.getString(1),rs.getString(2)),id);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Chave Pix não configurada");
  return rows.get(0);
 }
 @PutMapping("/pix/admin/{competitionId}")
 @PreAuthorize("hasRole('DEV')")
 public PixPayment save(@PathVariable long competitionId,@RequestBody PixPayment input) {
  if(input==null||input.holderName()==null||input.pixKey()==null||
   input.holderName().trim().isEmpty()||input.holderName().trim().length()>180||
   input.pixKey().trim().isEmpty()||input.pixKey().trim().length()>255)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe titular e chave Pix válidos");
  if(db.queryForObject("SELECT COUNT(*) FROM competitions WHERE id=?",Integer.class,competitionId)==0)
   throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Competição não encontrada");
  db.update("INSERT INTO competition_pix_payment (competition_id,holder_name,pix_key) VALUES (?,?,?) ON DUPLICATE KEY UPDATE holder_name=VALUES(holder_name),pix_key=VALUES(pix_key)",
   competitionId,input.holderName().trim(),input.pixKey().trim());
  return find(competitionId);
 }
 @DeleteMapping("/pix/admin/{competitionId}")
 @PreAuthorize("hasRole('DEV')")
 public void delete(@PathVariable long competitionId) {
  db.update("DELETE FROM competition_pix_payment WHERE competition_id=?",competitionId);
 }
}