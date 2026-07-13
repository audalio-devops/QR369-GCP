package br.com.ia369.prospecting_service.dto;

/**
 * Resposta formatada para o processamento de importação de arquivos de CNPJ.
 */
public record ImportacaoResponse(
        int totalLidos,
        int totalImportados,
        int totalDuplicados) {
}
