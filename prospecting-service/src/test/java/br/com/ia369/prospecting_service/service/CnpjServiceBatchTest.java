package br.com.ia369.prospecting_service.service;

import br.com.ia369.prospecting_service.client.BrasilApiClient;
import br.com.ia369.prospecting_service.dto.BrasilApiCnpjDTO;
import br.com.ia369.prospecting_service.dto.CNPJResponse;
import br.com.ia369.prospecting_service.dto.ImportacaoResponse;
import br.com.ia369.prospecting_service.entity.BuscaCnpj;
import br.com.ia369.prospecting_service.entity.Empresa;
import br.com.ia369.prospecting_service.repository.BuscaCnpjRepository;
import br.com.ia369.prospecting_service.repository.EmpresaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CnpjServiceBatchTest {

    private EmpresaRepository empresaRepository;
    private BuscaCnpjRepository buscaCnpjRepository;
    private BrasilApiClient brasilApiClient;
    private CnpjService cnpjService;

    @BeforeEach
    void setUp() {
        empresaRepository = Mockito.mock(EmpresaRepository.class);
        buscaCnpjRepository = Mockito.mock(BuscaCnpjRepository.class);
        brasilApiClient = Mockito.mock(BrasilApiClient.class);
        cnpjService = spy(new CnpjService(empresaRepository, buscaCnpjRepository, brasilApiClient));
    }

    @Test
    void deveNaoProcessarNadaQuandoFilaEstaVazia() {
        // given
        when(buscaCnpjRepository.findAll()).thenReturn(Collections.emptyList());

        // when
        cnpjService.processarLoteDeCnpjs();

        // then - nenhuma busca deve ser disparada
        verify(empresaRepository, never()).findById(anyString());
    }

    @Test
    void deveProcessarTodosOsCnpjsDaFila() {
        // given
        BuscaCnpj item1 = new BuscaCnpj();
        item1.setCnpj("60701190000104");

        BuscaCnpj item2 = new BuscaCnpj();
        item2.setCnpj("33000167000101");

        when(buscaCnpjRepository.findAll()).thenReturn(List.of(item1, item2));
        when(empresaRepository.findById("60701190000104")).thenReturn(Optional.empty());
        when(empresaRepository.findById("33000167000101")).thenReturn(Optional.empty());
        when(brasilApiClient.buscarCnpj(anyString())).thenReturn(Optional.empty());

        // when
        cnpjService.processarLoteDeCnpjs();

        // then - buscarPorCnpj deve ser chamado para cada item da fila
        verify(empresaRepository, times(1)).findById("60701190000104");
        verify(empresaRepository, times(1)).findById("33000167000101");
    }

    @Test
    void deveContinuarProcessandoMesmoDiranteErroIndividual() {
        // given
        BuscaCnpj itemComErro = new BuscaCnpj();
        itemComErro.setCnpj("00000000000001");

        BuscaCnpj itemValido = new BuscaCnpj();
        itemValido.setCnpj("33000167000101");

        when(buscaCnpjRepository.findAll()).thenReturn(List.of(itemComErro, itemValido));
        when(empresaRepository.findById("00000000000001"))
                .thenThrow(new RuntimeException("Erro simulado de banco de dados"));
        when(empresaRepository.findById("33000167000101")).thenReturn(Optional.empty());
        when(brasilApiClient.buscarCnpj("33000167000101")).thenReturn(Optional.empty());

        // when - nao deve lancar excecao
        cnpjService.processarLoteDeCnpjs();

        // then - o segundo item deve ter sido processado mesmo apos o erro do primeiro
        verify(empresaRepository, times(1)).findById("00000000000001");
        verify(empresaRepository, times(1)).findById("33000167000101");
    }

    @Test
    void deveRetornarDoBancoSeExistirLocalmenteSemChamarBrasilApi() {
        // given
        String cnpj = "12345678901234";
        Empresa empresa = Empresa.builder()
                .cnpj(cnpj)
                .razaoSocial("Empresa Local")
                .nomeFantasia("Fantasia Local")
                .build();

        when(empresaRepository.findById(cnpj)).thenReturn(Optional.of(empresa));

        // when
        Optional<CNPJResponse> result = cnpjService.buscarPorCnpj(cnpj);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().cnpj()).isEqualTo(cnpj);
        assertThat(result.get().razaoSocial()).isEqualTo("Empresa Local");
        verify(brasilApiClient, never()).buscarCnpj(cnpj);
    }

    @Test
    void deveBuscarNaBrasilApiESalvarLocalmenteSeNaoExistirNoBanco() {
        // given
        String cnpj = "12234452000117";
        BrasilApiCnpjDTO dto = new BrasilApiCnpjDTO(
                cnpj,
                "Empresa API",
                "Fantasia API",
                "ATIVA",
                "2010-01-01",
                "11 3550-3030",
                "contato@api.com",
                "Rua API",
                "123",
                "",
                "Bairro API",
                "Cidade API",
                "SP",
                "01001-000");

        Empresa empresaSalva = Empresa.builder()
                .cnpj(cnpj)
                .razaoSocial("Empresa API")
                .nomeFantasia("Fantasia API")
                .situacaoCadastral("ATIVA")
                .dataAbertura(LocalDate.of(2010, 1, 1))
                .ddd("11")
                .telefone("3550-3030")
                .email("contato@api.com")
                .logradouro("Rua API")
                .numero("123")
                .complemento("")
                .bairro("Bairro API")
                .cidade("Cidade API")
                .uf("SP")
                .cep("01001-000")
                .build();

        when(empresaRepository.findById(cnpj)).thenReturn(Optional.empty());
        when(brasilApiClient.buscarCnpj(cnpj)).thenReturn(Optional.of(dto));
        when(empresaRepository.save(any(Empresa.class))).thenReturn(empresaSalva);

        // when
        Optional<CNPJResponse> result = cnpjService.buscarPorCnpj(cnpj);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().cnpj()).isEqualTo(cnpj);
        assertThat(result.get().razaoSocial()).isEqualTo("Empresa API");
        verify(empresaRepository, times(1)).save(any(Empresa.class));
    }

    @Test
    void deveImportarCnpjsDeArquivoSanitizandoCorretamente() throws Exception {
        // given
        org.springframework.web.multipart.MultipartFile mockFile = Mockito
                .mock(org.springframework.web.multipart.MultipartFile.class);
        String conteudo = "12.234.452/0001-17\n  60701190000104 \n\n33.000.167/0001-01\n";

        when(mockFile.getOriginalFilename()).thenReturn("cnpjs.txt");
        when(mockFile.getInputStream()).thenReturn(
                new java.io.ByteArrayInputStream(conteudo.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        when(buscaCnpjRepository.existsByCnpj("12234452000117")).thenReturn(false);
        when(buscaCnpjRepository.existsByCnpj("60701190000104")).thenReturn(false);
        when(buscaCnpjRepository.existsByCnpj("33000167000101")).thenReturn(false);

        // when
        ImportacaoResponse response = cnpjService.importarCnpjs(mockFile);

        // then
        assertThat(response).isNotNull();
        assertThat(response.totalLidos()).isEqualTo(3);
        assertThat(response.totalImportados()).isEqualTo(3);
        assertThat(response.totalDuplicados()).isEqualTo(0);

        verify(buscaCnpjRepository, times(3)).save(any(BuscaCnpj.class));
    }

    @Test
    void deveIgnorarCnpjsDuplicadosJaSalvosNoBanco() throws Exception {
        // given
        org.springframework.web.multipart.MultipartFile mockFile = Mockito
                .mock(org.springframework.web.multipart.MultipartFile.class);
        String conteudo = "12.234.452/0001-17\n60701190000104\n";

        when(mockFile.getOriginalFilename()).thenReturn("cnpjs.csv");
        when(mockFile.getInputStream()).thenReturn(
                new java.io.ByteArrayInputStream(conteudo.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        when(buscaCnpjRepository.existsByCnpj("12234452000117")).thenReturn(true); // duplicado
        when(buscaCnpjRepository.existsByCnpj("60701190000104")).thenReturn(false); // novo

        // when
        ImportacaoResponse response = cnpjService.importarCnpjs(mockFile);

        // then
        assertThat(response).isNotNull();
        assertThat(response.totalLidos()).isEqualTo(2);
        assertThat(response.totalImportados()).isEqualTo(1);
        assertThat(response.totalDuplicados()).isEqualTo(1);

        verify(buscaCnpjRepository, times(1)).save(any(BuscaCnpj.class));
    }
}
