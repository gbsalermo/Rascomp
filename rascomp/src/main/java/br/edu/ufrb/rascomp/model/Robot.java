package br.edu.ufrb.rascomp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "robots", uniqueConstraints = {@UniqueConstraint(name = "uk_robot_nome_team", columnNames = {"nome", "team_id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Robot implements Serializable{

	private static final long seriaVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, length = 120)
	private String nome; 
	
	@Column(length = 500)
    private String descricao;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
	
	@Column(nullable = false)
    private Boolean ativo = true;
	
	@CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;
	
}
