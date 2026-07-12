package br.com.ia369.prospecting_service.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record BrasilApiEmpresaDTO(
        String cnpj,
        @JsonProperty("razao_social") String razaoSocial,
        @JsonProperty("nome_fantasia") String nomeFantasia,
        @JsonProperty("situacao_cadastral") String situacaoCadastral,
        @JsonProperty("data_inicio_atividade") LocalDate dataAbertura,
        @JsonProperty("ddd_telefone_1") String dddTelefone1,
        @JsonProperty("ddd_telefone_2") String dddTelefone2,
        String email,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        String cep) {
}
