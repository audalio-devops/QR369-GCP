package br.com.ia369.virtual_assistant.cnpj;

import java.time.LocalDate;

public record CNPJResponse(
    String cnpj,
    String razaoSocial,
    String nomeFantasia,
    String situacaoCadastral,
    LocalDate dataAbertura,
    String ddd,
    String telefone,
    String email,
    String logradouro,
    String numero,
    String complemento,
    String bairro,
    String cidade,
    String uf,
    String cep
) {}
