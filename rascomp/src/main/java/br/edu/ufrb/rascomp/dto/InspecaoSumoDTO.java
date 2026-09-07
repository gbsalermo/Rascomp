package br.edu.ufrb.rascomp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.InspecaoSumo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InspecaoSumoDTO {

    private Long id;

    @NotNull(message = "Inscrição é obrigatória")
    private Long registrationId;

    private Integer numeroTentativa;

    @DecimalMin(value = "0.001", message = "Peso medido deve ser maior que zero")
    private BigDecimal pesoMedido;

    @NotNull(message = "Informe o resultado humano da inspeção")
    private Boolean aprovada;

    @Size(max = 500, message = "Observação deve possuir no máximo 500 caracteres")
    private String observacao;

    private Long registradoPorId;
    private String registradoPorNome;
    private LocalDateTime dataCadastro;

    public InspecaoSumoDTO(InspecaoSumo entity) {
        this.id = entity.getId();
        this.registrationId = entity.getRegistration().getId();
        this.numeroTentativa = entity.getNumeroTentativa();
        this.pesoMedido = entity.getPesoMedido();
        this.aprovada = entity.getAprovada();
        this.observacao = entity.getObservacao();
        this.registradoPorId = entity.getRegistradoPor() != null ? entity.getRegistradoPor().getId() : null;
        this.registradoPorNome = entity.getRegistradoPor() != null ? entity.getRegistradoPor().getNome() : null;
        this.dataCadastro = entity.getDataCadastro();
    }
}
