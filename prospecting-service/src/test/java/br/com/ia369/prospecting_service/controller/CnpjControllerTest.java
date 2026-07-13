package br.com.ia369.prospecting_service.controller;

import br.com.ia369.prospecting_service.dto.CNPJResponse;
import br.com.ia369.prospecting_service.dto.ImportacaoResponse;
import br.com.ia369.prospecting_service.service.CnpjService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CnpjControllerTest {

    private CnpjService cnpjService;
    private CnpjController cnpjController;

    @BeforeEach
    void setUp() {
        cnpjService = Mockito.mock(CnpjService.class);
        cnpjController = new CnpjController(cnpjService);
    }

    @Test
    void deveRetornarCnpjSeCadastrado() {
        CNPJResponse mockResponse = new CNPJResponse(
                "60701190000104", "BANCO BRADESCO S.A.", "BRADESCO", "ATIVA",
                LocalDate.of(1943, 3, 10), "11", "37350000", "contato@bradesco.com.br",
                "Cidade de Deus", "s/n", "Predio Prata", "Vila Yara", "Osasco", "SP", "06029-900");

        when(cnpjService.buscarPorCnpj("60701190000104")).thenReturn(Optional.of(mockResponse));

        ResponseEntity<CNPJResponse> response = cnpjController.buscarPorCnpj("60701190000104");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().cnpj()).isEqualTo("60701190000104");
        assertThat(response.getBody().razaoSocial()).isEqualTo("BANCO BRADESCO S.A.");
    }

    @Test
    void deveRetornar404ParaCnpjInexistente() {
        when(cnpjService.buscarPorCnpj("99999999999999")).thenReturn(Optional.empty());

        ResponseEntity<CNPJResponse> response = cnpjController.buscarPorCnpj("99999999999999");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deveRetornar202AoDispararProcessamentoEmLote() {
        doNothing().when(cnpjService).processarLoteDeCnpjs();

        ResponseEntity<Void> response = cnpjController.processarLote();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(cnpjService).processarLoteDeCnpjs();
    }

    @Test
    void deveRetornarImportacaoResponseComSucesso() throws Exception {
        org.springframework.web.multipart.MultipartFile mockFile = Mockito
                .mock(org.springframework.web.multipart.MultipartFile.class);
        ImportacaoResponse expectedResponse = new ImportacaoResponse(10, 8, 2);

        when(cnpjService.importarCnpjs(mockFile)).thenReturn(expectedResponse);

        ResponseEntity<ImportacaoResponse> response = cnpjController.importarCnpj(mockFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalLidos()).isEqualTo(10);
        assertThat(response.getBody().totalImportados()).isEqualTo(8);
        assertThat(response.getBody().totalDuplicados()).isEqualTo(2);
    }

    @Test
    void deveRetornarBadRequestQuandoOcorrerErroNaImportacao() throws Exception {
        org.springframework.web.multipart.MultipartFile mockFile = Mockito
                .mock(org.springframework.web.multipart.MultipartFile.class);

        when(cnpjService.importarCnpjs(mockFile)).thenThrow(new java.io.IOException("Erro de teste"));

        ResponseEntity<ImportacaoResponse> response = cnpjController.importarCnpj(mockFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
