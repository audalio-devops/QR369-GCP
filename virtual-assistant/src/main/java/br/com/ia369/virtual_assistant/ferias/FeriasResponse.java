package br.com.ia369.virtual_assistant.ferias;

import java.time.LocalDate;

public record FeriasResponse(
        String matricula,
        LocalDate periodoInicio,
        LocalDate periodoFim,
        int diasDisponiveis) {
}
