package br.edu.ufrb.rascomp.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompetitionRegistrationWindowChangeRequest {
    @NotNull
    private LocalDate novaDataFim;

    @NotBlank
    @Size(max = 500)
    private String motivo;
}
