package br.com.ia369.prospecting_service.dto;

import br.com.ia369.prospecting_service.entity.Ferias;

import java.time.LocalDate;

public record FeriasResponse(
        String matricula,
        LocalDate periodoInicio,
        LocalDate periodoFim,
        int diasDisponiveis) {
    public static FeriasResponse from(Ferias ferias) {
        return new FeriasResponse(
                ferias.getMatricula(),
                ferias.getPeriodoInicio(),
                ferias.getPeriodoFim(),
                ferias.getDiasDisponiveis());
    }
}
