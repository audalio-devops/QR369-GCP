package br.com.ia369.prospecting_service.mapper;

import br.com.ia369.prospecting_service.dto.BrasilApiCnpjDTO;
import br.com.ia369.prospecting_service.entity.Empresa;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utilitário de conversão entre BrasilApiCnpjDTO e a entidade Empresa.
 */
public class EmpresaMapper {

    private EmpresaMapper() {
        // utility class — não instanciável
    }

    /**
     * Converte o DTO da BrasilAPI para a entidade JPA Empresa.
     *
     * @param dto resposta bruta da BrasilAPI
     * @return entidade Empresa pronta para persistência
     */
    public static Empresa toEntity(BrasilApiCnpjDTO dto) {
        String[] telefone = splitDddTelefone(dto.dddTelefone1());

        return Empresa.builder()
                .cnpj(dto.cnpj())
                .razaoSocial(dto.razaoSocial())
                .nomeFantasia(dto.nomeFantasia())
                .situacaoCadastral(dto.situacaoCadastral())
                .dataAbertura(parseData(dto.dataInicioAtividade()))
                .ddd(telefone[0])
                .telefone(telefone[1])
                .email(dto.email())
                .logradouro(dto.logradouro())
                .numero(dto.numero())
                .complemento(dto.complemento())
                .bairro(dto.bairro())
                .cidade(dto.municipio())
                .uf(dto.uf())
                .cep(dto.cep())
                .build();
    }

    /**
     * Separa o campo ddd_telefone_1 da BrasilAPI em DDD e número.
     * Formatos reconhecidos:
     * "11 3550-3030" → ["11", "3550-3030"]
     * "1135503030" → ["11", "35503030"]
     * null / "" → ["", ""]
     */
    static String[] splitDddTelefone(String dddTelefone) {
        if (dddTelefone == null || dddTelefone.isBlank()) {
            return new String[] { "", "" };
        }
        String valor = dddTelefone.trim();
        // Caso com espaço: "11 3550-3030"
        int espaco = valor.indexOf(' ');
        if (espaco == 2) {
            return new String[] { valor.substring(0, 2), valor.substring(3) };
        }
        // Caso compactado (ex: "1135503030" = DDD 2 dígitos + número)
        if (valor.length() >= 3) {
            return new String[] { valor.substring(0, 2), valor.substring(2) };
        }
        return new String[] { "", valor };
    }

    /**
     * Converte a string de data no formato "YYYY-MM-DD" para LocalDate.
     * Retorna null se o valor for nulo, vazio ou inválido.
     */
    static LocalDate parseData(String dataStr) {
        if (dataStr == null || dataStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dataStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
