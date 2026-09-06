package br.edu.ufrb.rascomp.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.CompetitionRegistrationWindowChange;
import br.edu.ufrb.rascomp.model.Enum.RegistrationWindowChangeType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompetitionRegistrationWindowChangeDTO {
    private Long id;
    private Long competitionId;
    private String competitionNome;
    private RegistrationWindowChangeType tipo;
    private LocalDate dataFimAnterior;
    private LocalDate novaDataFim;
    private String motivo;
    private Long realizadoPorId;
    private String realizadoPorNome;
    private LocalDateTime dataCadastro;

    public CompetitionRegistrationWindowChangeDTO(CompetitionRegistrationWindowChange entity) {
        this.id = entity.getId();
        this.competitionId = entity.getCompetition().getId();
        this.competitionNome = entity.getCompetition().getNome();
        this.tipo = entity.getTipo();
        this.dataFimAnterior = entity.getDataFimAnterior();
        this.novaDataFim = entity.getNovaDataFim();
        this.motivo = entity.getMotivo();
        this.realizadoPorId = entity.getRealizadoPor().getId();
        this.realizadoPorNome = entity.getRealizadoPor().getNome();
        this.dataCadastro = entity.getDataCadastro();
    }
}
