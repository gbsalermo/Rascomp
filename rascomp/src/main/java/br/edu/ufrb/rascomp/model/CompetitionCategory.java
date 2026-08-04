package br.edu.ufrb.rascomp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "competition_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionCategory implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, length = 100)
	private String nome;
		
	@Column(length = 500)
	private String descricao;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Modalidade modalidade;
	
	@Builder.Default
	@Column(nullable = false)
    private Boolean ativo = true;
	
	@OneToOne(mappedBy = "competitionCategory", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private ConfigSumo configSumo;
	
	@OneToOne(mappedBy = "competitionCategory", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private ConfigFollow configFollow;
	
	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime dataCadastro;
	
}
