package br.edu.ufrb.rascomp.service;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompetitionCategoryAssignmentService {
    private final JdbcTemplate db;

    public List<Long> list(Long competitionId) {
        return db.queryForList("SELECT category_id FROM competition_enabled_categories WHERE competition_id=? ORDER BY category_id",Long.class,competitionId);
    }

    @Transactional
    public void replace(Long competitionId,List<Long> categoryIds) {
        if (categoryIds==null) throw new IllegalArgumentException("Informe as categorias da competição.");
        if (db.queryForObject("SELECT COUNT(*) FROM competitions WHERE id=?",Integer.class,competitionId)==0)
            throw new IllegalArgumentException("Competição não encontrada.");
        Set<Long> selected=new HashSet<>(categoryIds);
        if (selected.size()!=categoryIds.size() || selected.contains(null))
            throw new IllegalArgumentException("Categorias duplicadas ou inválidas.");
        for(Long id:selected) {
            Integer available=db.queryForObject("SELECT COUNT(*) FROM competition_categories WHERE id=? AND ativo=TRUE",Integer.class,id);
            if(available==null||available==0)throw new IllegalArgumentException("Categoria inexistente ou inativa: "+id);
            String modality=db.queryForObject("SELECT modalidade FROM competition_categories WHERE id=?",String.class,id);
            if("FOLLOW_LINE".equals(modality)||"SUMO".equals(modality)){
                String table="FOLLOW_LINE".equals(modality)?"config_follow":"config_sumo";
                Integer configured=db.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE competition_category_id=?",Integer.class,id);
                if(configured==null||configured==0)throw new IllegalArgumentException(
                    "Categoria "+id+" ainda não possui configuração operacional de "+modality+".");
            }
        }
        List<Long> withRegistration=db.queryForList("SELECT DISTINCT category_id FROM registrations WHERE competition_id=?",Long.class,competitionId);
        if(!selected.containsAll(withRegistration))
            throw new IllegalArgumentException("Não é possível remover categoria com inscrições existentes.");
        db.update("DELETE FROM competition_enabled_categories WHERE competition_id=?",competitionId);
        for(Long id:selected)db.update("INSERT INTO competition_enabled_categories(competition_id,category_id) VALUES (?,?)",competitionId,id);
    }

    public void requireEnabled(Long competitionId,Long categoryId) {
        Integer count=db.queryForObject("SELECT COUNT(*) FROM competition_enabled_categories WHERE competition_id=? AND category_id=?",Integer.class,competitionId,categoryId);
        if(count==null||count==0)throw new IllegalArgumentException("Esta categoria não está habilitada na competição.");
    }
}
