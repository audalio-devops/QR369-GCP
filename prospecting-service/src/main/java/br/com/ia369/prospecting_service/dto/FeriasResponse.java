package br.com.ia369.prospecting_service.dto;

import br.com.ia369.prospecting_service.entity.Ferias;

import java.time.LocalDate;

// DTO de resposta do contrato REST consumido pelo hr-assistant.
public record FeriasResponse(
		String matricula,
		LocalDate periodoInicio,
		LocalDate periodoFim,
		int diasDisponiveis
) {

	// Converte a entidade em resposta da API.
	public static FeriasResponse from(Ferias ferias) {
		return new FeriasResponse(
				ferias.getMatricula(),
				ferias.getPeriodoInicio(),
				ferias.getPeriodoFim(),
				ferias.getDiasDisponiveis()
		);
	}
}
