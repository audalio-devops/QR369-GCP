package br.com.ia369.prospecting_service.controller;

import br.com.ia369.prospecting_service.model.ProspectingAudit;
import br.com.ia369.prospecting_service.service.ProspectingAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProspectingAccountControllerTest {

    private ProspectingAccountService service;
    private ProspectingAccountController controller;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(ProspectingAccountService.class);
        controller = new ProspectingAccountController(service);
    }

    @Test
    @DisplayName("getStatus deve delegar para verificarStatusEMonitorar e retornar 200 OK")
    void deveRetornarStatusMonitoramento() {
        Map<String, Object> mockStatus = Map.of(
                "running", false,
                "zapiConnected", true
        );
        when(service.verificarStatusEMonitorar()).thenReturn(mockStatus);

        ResponseEntity<Map<String, Object>> response = controller.getStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("running")).isEqualTo(false);
        assertThat(response.getBody().get("zapiConnected")).isEqualTo(true);
        verify(service).verificarStatusEMonitorar();
    }

    @Test
    @DisplayName("getRecentAuditLogs deve delegar para getRecentAuditLogs com limit especificado")
    void deveRetornarLogsAuditoriaComLimite() {
        ProspectingAudit audit = new ProspectingAudit();
        audit.setId(1L);
        audit.setStatus("Funcionando");

        when(service.getRecentAuditLogs(20)).thenReturn(List.of(audit));

        ResponseEntity<List<ProspectingAudit>> response = controller.getRecentAuditLogs(20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(service).getRecentAuditLogs(20);
    }
}
