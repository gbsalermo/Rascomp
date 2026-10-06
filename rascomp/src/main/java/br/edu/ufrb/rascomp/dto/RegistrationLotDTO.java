package br.edu.ufrb.rascomp.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.RegistrationLot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistrationLotDTO {

    private Long id;
    private Long competitionId;

    @NotBlank
    @Size(max = 100)
    private String nome;

    @NotNull
    private LocalDate dataInicio;

    @NotNull
    private LocalDate dataFim;

    private Boolean ativo;
    private Boolean atual;
    private LocalDateTime dataCadastro;

    public RegistrationLotDTO(RegistrationLot entity, LocalDate hoje) {
        id = entity.getId();
        competitionId = entity.getCompetition().getId();
        nome = entity.getNome();
        dataInicio = entity.getDataInicio();
        dataFim = entity.getDataFim();
        ativo = entity.getAtivo();
        atual = Boolean.TRUE.equals(entity.getAtivo())
                && !hoje.isBefore(entity.getDataInicio())
                && !hoje.isAfter(entity.getDataFim());
        dataCadastro = entity.getDataCadastro();
    }
}
