package br.com.ia369.prospecting_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

// Entidade que representa o periodo de ferias de um colaborador.
@Entity
@Table(name = "tb_ferias")
public class Ferias {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Matricula do colaborador, unica no sistema.
	@Column(unique = true, nullable = false)
	private String matricula;

	private LocalDate periodoInicio;

	private LocalDate periodoFim;

	private int diasDisponiveis;

	public Ferias() {
	}

	public Ferias(String matricula, LocalDate periodoInicio, LocalDate periodoFim, int diasDisponiveis) {
		this.matricula = matricula;
		this.periodoInicio = periodoInicio;
		this.periodoFim = periodoFim;
		this.diasDisponiveis = diasDisponiveis;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getMatricula() {
		return matricula;
	}

	public void setMatricula(String matricula) {
		this.matricula = matricula;
	}

	public LocalDate getPeriodoInicio() {
		return periodoInicio;
	}

	public void setPeriodoInicio(LocalDate periodoInicio) {
		this.periodoInicio = periodoInicio;
	}

	public LocalDate getPeriodoFim() {
		return periodoFim;
	}

	public void setPeriodoFim(LocalDate periodoFim) {
		this.periodoFim = periodoFim;
	}

	public int getDiasDisponiveis() {
		return diasDisponiveis;
	}

	public void setDiasDisponiveis(int diasDisponiveis) {
		this.diasDisponiveis = diasDisponiveis;
	}
}
