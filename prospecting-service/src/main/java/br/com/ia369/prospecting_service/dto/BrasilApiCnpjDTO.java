package br.com.ia369.prospecting_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO que representa a resposta bruta da BrasilAPI para consulta de CNPJ.
 * Endpoint: GET https://brasilapi.com.br/api/cnpj/v1/{cnpj}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrasilApiCnpjDTO(

        @JsonProperty("cnpj") String cnpj,

        @JsonProperty("razao_social") String razaoSocial,

        @JsonProperty("nome_fantasia") String nomeFantasia,

        @JsonProperty("descricao_situacao_cadastral") String situacaoCadastral,

        @JsonProperty("data_inicio_atividade") String dataInicioAtividade,

        /**
         * Formato retornado pela BrasilAPI: "XX NNNNN-NNNN" ou "XXXXXXXXXXX"
         * ex: "11 3550-3030" ou "1135503030"
         */
        @JsonProperty("ddd_telefone_1") String dddTelefone1,

        @JsonProperty("email") String email,

        @JsonProperty("logradouro") String logradouro,

        @JsonProperty("numero") String numero,

        @JsonProperty("complemento") String complemento,

        @JsonProperty("bairro") String bairro,

        @JsonProperty("municipio") String municipio,

        @JsonProperty("uf") String uf,

        @JsonProperty("cep") String cep) {
}
