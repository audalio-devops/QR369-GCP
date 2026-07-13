package br.com.ia369.prospecting_service.mapper;

import br.com.ia369.prospecting_service.dto.BrasilApiCnpjDTO;
import br.com.ia369.prospecting_service.entity.Empresa;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EmpresaMapperTest {

    @Test
    void deveMapearDtoParaEntidadeCorretamente() {
        // given
        BrasilApiCnpjDTO dto = new BrasilApiCnpjDTO(
                "12345678000199",
                "RAZAO SOCIAL TESTE LTDA",
                "NOME FANTASIA TESTE",
                "ATIVA",
                "2021-05-15",
                "11 99999-8888",
                "teste@empresa.com",
                "Rua das Flores",
                "100",
                "Salas 1 e 2",
                "Centro",
                "São Paulo",
                "SP",
                "01001-000");

        // when
        Empresa empresa = EmpresaMapper.toEntity(dto);

        // then
        assertThat(empresa).isNotNull();
        assertThat(empresa.getCnpj()).isEqualTo("12345678000199");
        assertThat(empresa.getRazaoSocial()).isEqualTo("RAZAO SOCIAL TESTE LTDA");
        assertThat(empresa.getNomeFantasia()).isEqualTo("NOME FANTASIA TESTE");
        assertThat(empresa.getSituacaoCadastral()).isEqualTo("ATIVA");
        assertThat(empresa.getDataAbertura()).isEqualTo(LocalDate.of(2021, 5, 15));
        assertThat(empresa.getDdd()).isEqualTo("11");
        assertThat(empresa.getTelefone()).isEqualTo("99999-8888");
        assertThat(empresa.getEmail()).isEqualTo("teste@empresa.com");
        assertThat(empresa.getLogradouro()).isEqualTo("Rua das Flores");
        assertThat(empresa.getNumero()).isEqualTo("100");
        assertThat(empresa.getComplemento()).isEqualTo("Salas 1 e 2");
        assertThat(empresa.getBairro()).isEqualTo("Centro");
        assertThat(empresa.getCidade()).isEqualTo("São Paulo");
        assertThat(empresa.getUf()).isEqualTo("SP");
        assertThat(empresa.getCep()).isEqualTo("01001-000");
    }

    @Test
    void deveTratarTelefoneSemEspaco() {
        // given
        BrasilApiCnpjDTO dto = new BrasilApiCnpjDTO(
                "12345678000199", "Razao", "Fantasia", "ATIVA", "2021-05-15",
                "11988887777", "email", "rua", "1", "", "bairro", "cidade", "SP", "00000000");

        // when
        Empresa empresa = EmpresaMapper.toEntity(dto);

        // then
        assertThat(empresa.getDdd()).isEqualTo("11");
        assertThat(empresa.getTelefone()).isEqualTo("988887777");
    }

    @Test
    void deveTratarTelefoneNuloOuVazio() {
        // given
        BrasilApiCnpjDTO dto1 = new BrasilApiCnpjDTO(
                "12345678000199", "Razao", "Fantasia", "ATIVA", "2021-05-15",
                null, "email", "rua", "1", "", "bairro", "cidade", "SP", "00000000");
        BrasilApiCnpjDTO dto2 = new BrasilApiCnpjDTO(
                "12345678000199", "Razao", "Fantasia", "ATIVA", "2021-05-15",
                "   ", "email", "rua", "1", "", "bairro", "cidade", "SP", "00000000");

        // when
        Empresa empresa1 = EmpresaMapper.toEntity(dto1);
        Empresa empresa2 = EmpresaMapper.toEntity(dto2);

        // then
        assertThat(empresa1.getDdd()).isEmpty();
        assertThat(empresa1.getTelefone()).isEmpty();
        assertThat(empresa2.getDdd()).isEmpty();
        assertThat(empresa2.getTelefone()).isEmpty();
    }

    @Test
    void deveTratarDataAberturaInvalida() {
        // given
        BrasilApiCnpjDTO dto = new BrasilApiCnpjDTO(
                "12345678000199", "Razao", "Fantasia", "ATIVA", "data-invalida",
                "11988887777", "email", "rua", "1", "", "bairro", "cidade", "SP", "00000000");

        // when
        Empresa empresa = EmpresaMapper.toEntity(dto);

        // then
        assertThat(empresa.getDataAbertura()).isNull();
    }
}
