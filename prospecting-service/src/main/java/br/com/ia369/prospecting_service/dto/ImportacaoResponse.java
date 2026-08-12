package br.com.ia369.prospecting_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportacaoResponse {
    private String status;
    private long registrosImportados;
    private String mensagem;
}
