package br.com.ia369.prospecting_service.service;

import br.com.ia369.prospecting_service.client.ZApiClient;
import br.com.ia369.prospecting_service.exception.ZApiDisconnectedException;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

        when(dataSourceRepository.findByStatusIsNullOrderByPrioridadeAsc()).thenReturn(List.of(testLead));
        when(phoneValidationService.validarTelefone(any(), any(), any())).thenReturn(
                PhoneValidationService.ResultadoValidacaoTelefone.aptoParaContato("5511999998888"));
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

        when(dataSourceRepository.findByStatusIsNullOrderByPrioridadeAsc()).thenReturn(List.of(leadSemWhatsapp, proximoLead));
        when(phoneValidationService.validarTelefone(eq("11999990000"), isNull(), any())).thenReturn(
                PhoneValidationService.ResultadoValidacaoTelefone.nenhumTelefoneValido());
        when(phoneValidationService.validarTelefone(eq("11999991111"), isNull(), any())).thenReturn(
                PhoneValidationService.ResultadoValidacaoTelefone.aptoParaContato("5511999991111"));
        when(messageService.sortearMensagem()).thenReturn("Ola Contador");

        service.startProspecting();

        assertTrue(temposDeEspera.isEmpty(), "Nao deve haver espera antes da leitura do proximo lead");
        verify(zApiClient).sendTextMessage("5511999991111", "Ola Contador");
    }

    @Test
    @DisplayName("Não deve enviar mensagem para número já contactado")
    void naoDeveEnviarMensagemParaNumeroJaContactado() {
        ProspectingDataSource lead = new ProspectingDataSource();
        lead.setCnpj("33333333000133");
        lead.setRazaoSocial("TesteControladoContactado");
        lead.setTelefone1("11999992222");

        when(dataSourceRepository.findByStatusIsNullOrderByPrioridadeAsc()).thenReturn(List.of(lead));
        when(phoneValidationService.validarTelefone(eq("11999992222"), isNull(), any())).thenReturn(
                PhoneValidationService.ResultadoValidacaoTelefone.jaContactado("55119999992222"));

        service.startProspecting();

        assertEquals("Número já contactado", lead.getStatus());
        verify(dataSourceRepository).save(lead);
        verify(zApiClient, never()).sendTextMessage(anyString(), anyString());
        verify(processedRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve inicializar ProspectingDataSource com prioridade default igual a 1 e consultar com ordenacao ascendente")
    void deveInicializarComPrioridadeDefault1EConsultarOrdenado() {
        ProspectingDataSource novoLead = new ProspectingDataSource();
        assertEquals(1, novoLead.getPrioridade(), "Prioridade default deve ser 1");

        ProspectingDataSource leadPrio1 = new ProspectingDataSource();
        leadPrio1.setCnpj("11111111000111");
        leadPrio1.setRazaoSocial("TesteControladoPrio1");
        leadPrio1.setTelefone1("11999991111");
        leadPrio1.setPrioridade(1);

        ProspectingDataSource leadPrio2 = new ProspectingDataSource();
        leadPrio2.setCnpj("22222222000122");
        leadPrio2.setRazaoSocial("TesteControladoPrio2");
        leadPrio2.setTelefone1("11999992222");
        leadPrio2.setPrioridade(2);

        when(dataSourceRepository.findByStatusIsNullOrderByPrioridadeAsc()).thenReturn(List.of(leadPrio1, leadPrio2));
        when(phoneValidationService.validarTelefone(any(), any(), any())).thenReturn(
                PhoneValidationService.ResultadoValidacaoTelefone.nenhumTelefoneValido());

        service.startProspecting();

        verify(dataSourceRepository).findByStatusIsNullOrderByPrioridadeAsc();
        assertEquals("Nenhum telefone válido", leadPrio1.getStatus());
        assertEquals("Nenhum telefone válido", leadPrio2.getStatus());
    }

    @Test
    @DisplayName("Deve parar a prospecção imediatamente, registrar erro na auditoria e não alterar status do lead quando a Z-API estiver desconectada")
    void devePararProspeccaoQuandoZApiDesconectada() {
        ProspectingDataSource lead1 = new ProspectingDataSource();
        lead1.setCnpj("11111111000111");
        lead1.setRazaoSocial("TesteControladoLead1");
        lead1.setTelefone1("11999991111");

        ProspectingDataSource lead2 = new ProspectingDataSource();
        lead2.setCnpj("22222222000122");
        lead2.setRazaoSocial("TesteControladoLead2");
        lead2.setTelefone1("11999992222");

        when(dataSourceRepository.findByStatusIsNullOrderByPrioridadeAsc()).thenReturn(List.of(lead1, lead2));
        when(phoneValidationService.validarTelefone(eq("11999991111"), isNull(), any()))
                .thenThrow(new ZApiDisconnectedException("Erro: Instância Web Z-API desconectada"));

        service.startProspecting();

        // 1. Deve gravar auditoria de Erro com a mensagem especificada e CNPJ do lead
        verify(auditRepository, atLeastOnce()).save(auditCaptor.capture());
        List<ProspectingAudit> audits = auditCaptor.getAllValues();
        assertTrue(audits.stream().anyMatch(a ->
                "Erro".equals(a.getStatus()) &&
                "Erro: Instância Web Z-API desconectada".equals(a.getLog()) &&
                "11111111000111".equals(a.getCnpj())
        ));

        // 2. Não deve alterar nem salvar o status do lead no dataSourceRepository
        verify(dataSourceRepository, never()).save(lead1);
        verify(dataSourceRepository, never()).save(lead2);
        assertNull(lead1.getStatus(), "O status do lead 1 não deve ser corrompido com 'Nenhum telefone válido'");
        assertNull(lead2.getStatus(), "O lead 2 sequer deve ter sido tocado");

        // 3. Não deve tentar validar o lead 2 (processo parado na sequência)
        verify(phoneValidationService, never()).validarTelefone(eq("11999992222"), isNull(), any());

        // 4. lastError deve estar preenchido
        assertEquals("Erro: Instância Web Z-API desconectada", service.getLastError());
        assertFalse(service.isRunning());
    }

    @Test
    @DisplayName("Deve buscar logs de auditoria recentes respeitando limite solicitado")
    void deveBuscarLogsAuditoriaRecentesComLimite() {
        ProspectingAudit audit1 = new ProspectingAudit();
        audit1.setId(1L);
        audit1.setStatus("Iniciado");

        when(auditRepository.findByOrderByDataEventoDesc(org.springframework.data.domain.PageRequest.of(0, 30)))
                .thenReturn(List.of(audit1));

        List<ProspectingAudit> logs = service.getRecentAuditLogs(30);

        assertEquals(1, logs.size());
        assertEquals("Iniciado", logs.get(0).getStatus());
        verify(auditRepository).findByOrderByDataEventoDesc(org.springframework.data.domain.PageRequest.of(0, 30));
    }

    @Test
    @DisplayName("Deve verificar status e monitorar com sucesso quando Z-API estiver conectada")
    void testVerificarStatusEMonitorarComZApiConectada() {
        when(zApiClient.isConnected()).thenReturn(true);

        Map<String, Object> status = service.verificarStatusEMonitorar();

        assertFalse((Boolean) status.get("running"));
        assertTrue((Boolean) status.get("zapiConnected"));
        assertNull(status.get("lastError"));

        verify(auditRepository).save(auditCaptor.capture());
        ProspectingAudit audit = auditCaptor.getValue();
        assertEquals("Parado", audit.getStatus());
        assertEquals("=== Monitoramento EXECUTADO - PARADO ===", audit.getLog());
    }

    @Test
    @DisplayName("Deve verificar status e registrar erro quando Z-API estiver desconectada")
    void testVerificarStatusEMonitorarComZApiDesconectada() {
        when(zApiClient.isConnected()).thenReturn(false);

        Map<String, Object> status = service.verificarStatusEMonitorar();

        assertFalse((Boolean) status.get("running"));
        assertFalse((Boolean) status.get("zapiConnected"));
        assertEquals("Erro: Instância Web Z-API desconectada", status.get("lastError"));

        verify(auditRepository).save(auditCaptor.capture());
        ProspectingAudit audit = auditCaptor.getValue();
        assertEquals("Erro", audit.getStatus());
        assertEquals("Erro: Instância Web Z-API desconectada", audit.getLog());
    }
}
