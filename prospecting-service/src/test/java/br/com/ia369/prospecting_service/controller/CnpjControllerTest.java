package br.com.ia369.prospecting_service.controller;

import br.com.ia369.prospecting_service.dto.CNPJResponse;
import br.com.ia369.prospecting_service.service.CnpjService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
                "Cidade de Deus", "s/n", "Prédio Prata", "Vila Yara", "Osasco", "SP", "06029-900");

        when(cnpjService.findByCnpj("60701190000104")).thenReturn(Optional.of(mockResponse));

        ResponseEntity<CNPJResponse> response = cnpjController.findByCnpj("60701190000104");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().cnpj()).isEqualTo("60701190000104");
        assertThat(response.getBody().razaoSocial()).isEqualTo("BANCO BRADESCO S.A.");
    }

    @Test
    void deveRetornar404ParaCnpjInexistente() {
        when(cnpjService.findByCnpj("99999999999999")).thenReturn(Optional.empty());

        ResponseEntity<CNPJResponse> response = cnpjController.findByCnpj("99999999999999");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
