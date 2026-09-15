package br.com.ia369.prospecting_service.service;

import br.com.ia369.prospecting_service.client.ZApiClient;
import br.com.ia369.prospecting_service.model.ProspectingAudit;
import br.com.ia369.prospecting_service.model.ProspectingDataSource;
import br.com.ia369.prospecting_service.repository.ProspectingAuditRepository;
import br.com.ia369.prospecting_service.repository.ProspectingDataSourceRepository;
import br.com.ia369.prospecting_service.repository.ProspectingProcessedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProspectingAccountServiceTest {

    @Mock
    private ProspectingDataSourceRepository dataSourceRepository;

    @Mock
    private ProspectingProcessedRepository processedRepository;

    @Mock
    private ProspectingAuditRepository auditRepository;

    @Mock
    private PhoneValidationService phoneValidationService;

    @Mock
    private MessageService messageService;

    @Mock
    private ZApiClient zApiClient;

    @Captor
    private ArgumentCaptor<ProspectingAudit> auditCaptor;

    private ProspectingAccountService service;
    private List<Long> temposDeEspera;

    @BeforeEach
    void setUp() {
        temposDeEspera = new ArrayList<>();
        service = new ProspectingAccountService(
                dataSourceRepository,
                processedRepository,
                auditRepository,
                phoneValidationService,
                messageService,
                zApiClient) {
            @Override
            protected void executarEspera(long millis) {
                temposDeEspera.add(millis);
                // Não dorme durante testes unitários, mas valida que o tempo é estritamente > 0
                assertTrue(millis >= 6 * 60 * 1000L && millis <= 20 * 60 * 1000L,
                        "O tempo sorteado deve ser no mínimo 6 min (5 + Random(1..15)) e no máximo 20 min");
            }
        };
    }

    @Test
    @DisplayName("Deve registrar audit 'Funcionando' se running=true e 'Parado' se running=false")
    void testRegistrarAuditMonitoramento() {
        service.registrarAuditMonitoramento(true);

        verify(auditRepository).save(auditCaptor.capture());
        ProspectingAudit auditRunning = auditCaptor.getValue();
        assertEquals("Funcionando", auditRunning.getStatus());
        assertEquals("=== Monitoramento EXECUTADO - EM EXECUÇÃO ===", auditRunning.getLog());
        assertNotNull(auditRunning.getDataEvento());

        reset(auditRepository);

        service.registrarAuditMonitoramento(false);
        verify(auditRepository).save(auditCaptor.capture());
        ProspectingAudit auditStopped = auditCaptor.getValue();
        assertEquals("Parado", auditStopped.getStatus());
        assertEquals("=== Monitoramento EXECUTADO - PARADO ===", auditStopped.getLog());
    }

    @Test
    @DisplayName("Deve processar lead de TesteControlado ignorando trava de horario e mantendo sorteio de tempo T = 5 + Random(1..15)")
    void testProcessarLeadTesteControlado() {
        ProspectingDataSource testLead = new ProspectingDataSource();
        testLead.setCnpj("12345678000199");
        testLead.setRazaoSocial("TesteControlado1");
        testLead.setTelefone1("11999998888");

        when(dataSourceRepository.findByStatusIsNull()).thenReturn(List.of(testLead));
        when(phoneValidationService.validarTelefone(any(), any())).thenReturn(Optional.of("5511999998888"));
        when(messageService.sortearMensagem()).thenReturn("Olá Contador");

        service.startProspecting();

        // Verifica que salvou auditoria de "Iniciado" e "Finalizado"
        verify(auditRepository, atLeast(2)).save(auditCaptor.capture());
        List<ProspectingAudit> savedAudits = auditCaptor.getAllValues();

        assertTrue(savedAudits.stream().anyMatch(a -> "Iniciado".equals(a.getStatus())));
        assertTrue(savedAudits.stream().anyMatch(a -> "Finalizado".equals(a.getStatus())));
        verify(zApiClient).sendTextMessage(eq("5511999998888"), anyString());
    }

    @Test
    @DisplayName("Nao deve sortear intervalo quando o lead nao possui telefone no WhatsApp")
    void naoDeveAguardarAposLeadSemTelefoneValido() {
        ProspectingDataSource leadSemWhatsapp = new ProspectingDataSource();
        leadSemWhatsapp.setCnpj("11111111000111");
        leadSemWhatsapp.setRazaoSocial("TesteControladoSemWhatsapp");
        leadSemWhatsapp.setTelefone1("11999990000");

        ProspectingDataSource proximoLead = new ProspectingDataSource();
        proximoLead.setCnpj("22222222000122");
        proximoLead.setRazaoSocial("TesteControladoComWhatsapp");
        proximoLead.setTelefone1("11999991111");

        when(dataSourceRepository.findByStatusIsNull()).thenReturn(List.of(leadSemWhatsapp, proximoLead));
        when(phoneValidationService.validarTelefone("11999990000", null)).thenReturn(Optional.empty());
        when(phoneValidationService.validarTelefone("11999991111", null)).thenReturn(Optional.of("5511999991111"));
        when(messageService.sortearMensagem()).thenReturn("Ola Contador");

        service.startProspecting();

        assertTrue(temposDeEspera.isEmpty(), "Nao deve haver espera antes da leitura do proximo lead");
        verify(zApiClient).sendTextMessage("5511999991111", "Ola Contador");
    }
}
