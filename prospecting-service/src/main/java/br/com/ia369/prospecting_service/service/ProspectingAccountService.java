package br.com.ia369.prospecting_service.service;

import br.com.ia369.prospecting_service.client.ZApiClient;
import br.com.ia369.prospecting_service.model.ProspectingAudit;
import br.com.ia369.prospecting_service.model.ProspectingDataSource;
import br.com.ia369.prospecting_service.model.ProspectingProcessed;
import br.com.ia369.prospecting_service.repository.ProspectingAuditRepository;
import br.com.ia369.prospecting_service.repository.ProspectingDataSourceRepository;
import br.com.ia369.prospecting_service.repository.ProspectingProcessedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Serviço principal de prospecção de contadores.
 *
 * Fluxo:
 * 1. Busca leads sem status em prospecting_data_source
 * 2. Para cada lead:
 * a. Verifica horário de funcionamento (Seg-Sex 08:00–16:40)
 * b. Valida telefones via Z-API
 * c. Salva em prospecting_processed
 * d. Envia mensagem
 * e. Registra em prospecting_audit
 * f. Aguarda intervalo aleatório (5–22 min)
 */
@Service
public class ProspectingAccountService {

    private static final Logger log = LoggerFactory.getLogger(ProspectingAccountService.class);
    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");

    private static final String STATUS_NENHUM_TELEFONE = "Nenhum telefone válido";
    private static final String STATUS_CONTATO_INICIAL = "Contato Inicial";
    private static final String AUDIT_OK = "Ok";
    private static final String AUDIT_ERROR = "Error";

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);

    private final ProspectingDataSourceRepository dataSourceRepository;
    private final ProspectingProcessedRepository processedRepository;
    private final ProspectingAuditRepository auditRepository;
    private final PhoneValidationService phoneValidationService;
    private final MessageService messageService;
    private final ZApiClient zApiClient;

    @Value("${prospecting.working-hours.start:08:00}")
    private String workingStart;

    @Value("${prospecting.working-hours.end:16:40}")
    private String workingEnd;

    @Value("${prospecting.interval.min:5}")
    private int intervalMin = 5;

    @Value("${prospecting.interval.max:22}")
    private int intervalMax = 22;

    private final Random random = new Random();

    public ProspectingAccountService(
            ProspectingDataSourceRepository dataSourceRepository,
            ProspectingProcessedRepository processedRepository,
            ProspectingAuditRepository auditRepository,
            PhoneValidationService phoneValidationService,
            MessageService messageService,
            ZApiClient zApiClient) {
        this.dataSourceRepository = dataSourceRepository;
        this.processedRepository = processedRepository;
        this.auditRepository = auditRepository;
        this.phoneValidationService = phoneValidationService;
        this.messageService = messageService;
        this.zApiClient = zApiClient;
    }

    /**
     * Verifica se uma execução já está ativa.
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Sinaliza para a execução corrente que ela deve parar.
     */
    public void stop() {
        String msg = "Sinal de parada recebido. A prospecção será encerrada após o registro atual.";
        log.info(msg);
        registrarAuditoria("Parado", msg, null);
        shouldStop.set(true);
    }

    /**
     * Ponto de entrada: executa o loop de prospecção de forma assíncrona.
     */
    @Async
    public void startProspecting() {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Prospecção já está em andamento. Ignorando nova chamada.");
            return;
        }
        shouldStop.set(false);

        String msgInicioEndpoint = "Prospecção de contadores iniciada via endpoint.";
        String msgInicio = "=== Prospecção de Contadores INICIADA ===";
        log.info(msgInicioEndpoint);
        registrarAuditoria("Iniciado", msgInicioEndpoint, null);
        log.info(msgInicio);
        registrarAuditoria("Iniciado", msgInicio, null);

        try {
            List<ProspectingDataSource> leads = dataSourceRepository.findByStatusIsNull();
            String msgLeads = "Leads pendentes encontrados: " + leads.size();
            log.info(msgLeads);
            registrarAuditoria("Iniciado", msgLeads, null);

            for (int i = 0; i < leads.size(); i++) {
                ProspectingDataSource lead = leads.get(i);

                if (shouldStop.get()) {
                    String msgParada = "Parada solicitada. Encerrando loop.";
                    log.info(msgParada);
                    registrarAuditoria("Parado", msgParada, null);
                    break;
                }

                boolean ehTesteControlado = isTesteControlado(lead);

                if (!ehTesteControlado && !dentroDoHorario()) {
                    String msgForaHorario = "Fora do horário de funcionamento. Encerrando execução.";
                    log.info(msgForaHorario);
                    registrarAuditoria("Erro", msgForaHorario, null);
                    break;
                }

                try {
                    boolean mensagemEnviada = processarLead(lead);
                    if (mensagemEnviada && (i + 1) < leads.size() && !shouldStop.get()) {
                        aguardarIntervalo();
                    }
                } catch (Exception ex) {
                    String cnpjLead = (lead != null) ? lead.getCnpj() : null;
                    String msgErroLead = "Falha ao processar lead (CNPJ=" + cnpjLead + "): " + ex.getMessage()
                            + ". Continuando para o próximo lead.";
                    log.error(msgErroLead, ex);
                    registrarAuditoria("Erro", msgErroLead, cnpjLead);
                }

            }
        } finally {
            isRunning.set(false);
            shouldStop.set(false);
            String msgFim = "=== Prospecção de Contadores FINALIZADA ===";
            log.info(msgFim);
            registrarAuditoria("Finalizado", msgFim, null);
        }
    }

    /**
     * Registra auditoria para o evento de monitoramento (GET
     * /prospecting-account/status).
     */
    public void registrarAuditMonitoramento(boolean isRunning) {
        String status = isRunning ? "Funcionando" : "Parado";
        String logMsg = isRunning ? "=== Monitoramento EXECUTADO - EM EXECUÇÃO ==="
                : "=== Monitoramento EXECUTADO - PARADO ===";
        registrarAuditoria(status, logMsg, null);
    }

    /**
     * Retorna os 10 logs de auditoria mais recentes.
     */
    public List<ProspectingAudit> getTop10AuditLogs() {
        return auditRepository.findTop10ByOrderByDataEventoDesc();
    }

    /**
     * Verifica se o lead pertence ao teste controlado (razao_social iniciando em
     * TesteControlado).
     */
    private boolean isTesteControlado(ProspectingDataSource lead) {
        return lead != null && lead.getRazaoSocial() != null && lead.getRazaoSocial().startsWith("TesteControlado");
    }

    /**
     * Método utilitário para gravação de auditoria genérica.
     */
    private void registrarAuditoria(String status, String logMessage, String cnpj) {
        try {
            ProspectingAudit audit = new ProspectingAudit();
            audit.setStatus(status);
            audit.setLog(logMessage);
            audit.setCnpj(cnpj);
            audit.setDataEvento(LocalDateTime.now(ZONE_SP));
            auditRepository.save(audit);
        } catch (Exception ex) {
            log.error("Falha ao registrar auditoria (status={}): {}", status, ex.getMessage(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Processamento individual de um lead
    // -------------------------------------------------------------------------

    private boolean processarLead(ProspectingDataSource lead) {
        String msgProcessando = "Processando lead: CNPJ=" + lead.getCnpj();
        log.info(msgProcessando);
        registrarAuditoria("Funcionando", msgProcessando, lead.getCnpj());

        Optional<String> telefoneValidoOpt = phoneValidationService.validarTelefone(
                lead.getTelefone1(), lead.getTelefone2());

        if (telefoneValidoOpt.isEmpty()) {
            // Nenhum telefone válido
            lead.setStatus(STATUS_NENHUM_TELEFONE);
            dataSourceRepository.save(lead);
            String msgSemTel = "CNPJ " + lead.getCnpj() + ": nenhum telefone válido.";
            log.info(msgSemTel);
            registrarAuditoria("Erro", msgSemTel, lead.getCnpj());
            return false;
        }

        String telefoneValido = telefoneValidoOpt.get();

        // Atualizar data source com o telefone válido
        lead.setStatus(telefoneValido);
        dataSourceRepository.save(lead);

        // Salvar em prospecting_processed (sem data_contato ainda)
        ProspectingProcessed processed = new ProspectingProcessed();
        processed.setCnpj(lead.getCnpj());
        processed.setEmail(lead.getEmail());
        processed.setRazaoSocial(lead.getRazaoSocial());
        processed.setTelefoneValido(telefoneValido);
        processedRepository.save(processed);

        // Enviar mensagem e registrar auditoria
        return enviarMensagem(lead.getCnpj(), telefoneValido, processed);
    }

    private boolean enviarMensagem(String cnpj, String telefoneValido, ProspectingProcessed processed) {
        ProspectingAudit audit = new ProspectingAudit();
        audit.setCnpj(cnpj);
        audit.setDataEvento(LocalDateTime.now(ZONE_SP));

        try {
            String mensagem = messageService.sortearMensagem();
            zApiClient.sendTextMessage(telefoneValido, mensagem);

            // Sucesso: atualizar registro processado
            processed.setDataContato(LocalDateTime.now(ZONE_SP));
            processed.setStatus(STATUS_CONTATO_INICIAL);
            processedRepository.save(processed);

            String msgSucesso = "CNPJ " + cnpj + ": mensagem enviada com sucesso para " + telefoneValido + ".";
            audit.setStatus(AUDIT_OK);
            audit.setLog(msgSucesso);
            log.info(msgSucesso);
            return true;

        } catch (Exception ex) {
            String msgErro = "CNPJ " + cnpj + ": falha ao enviar mensagem. Erro: " + ex.getMessage();
            audit.setStatus(AUDIT_ERROR);
            audit.setLog(msgErro);
            log.error(msgErro, ex);
            return false;
        } finally {
            auditRepository.save(audit);
        }
    }

    // -------------------------------------------------------------------------
    // Controle de horário e intervalo
    // -------------------------------------------------------------------------

    private boolean dentroDoHorario() {
        LocalDateTime agora = LocalDateTime.now(ZONE_SP);
        DayOfWeek dia = agora.getDayOfWeek();

        // Apenas segunda a sexta
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime horaAtual = agora.toLocalTime();
        LocalTime inicio = LocalTime.parse(workingStart);
        LocalTime fim = LocalTime.parse(workingEnd);

        return !horaAtual.isBefore(inicio) && !horaAtual.isAfter(fim);
    }

    private void aguardarIntervalo() {
        int baseMin = (intervalMin > 0) ? intervalMin : 5;
        int t = baseMin + (random.nextInt(15) + 1); // Sortear T = 5 + Random(1..15) minutos
        String msgEspera = "Aguardando " + t + " minuto(s) antes do próximo registro...";
        log.info(msgEspera);
        registrarAuditoria("Funcionando", msgEspera, null);
        executarEspera((long) t * 60 * 1000);
    }

    protected void executarEspera(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Espera interrompida: {}", ex.getMessage());
        }
    }
}
