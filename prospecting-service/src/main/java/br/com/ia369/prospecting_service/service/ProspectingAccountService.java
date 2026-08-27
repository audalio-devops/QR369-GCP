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
    private int intervalMin;

    @Value("${prospecting.interval.max:22}")
    private int intervalMax;

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
        log.info("Sinal de parada recebido. A prospecção será encerrada após o registro atual.");
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
        log.info("=== Prospecção de Contadores INICIADA ===");

        try {
            List<ProspectingDataSource> leads = dataSourceRepository.findByStatusIsNull();
            log.info("Leads pendentes encontrados: {}", leads.size());

            for (ProspectingDataSource lead : leads) {
                if (shouldStop.get()) {
                    log.info("Parada solicitada. Encerrando loop.");
                    break;
                }

                if (!dentroDoHorario()) {
                    log.info("Fora do horário de funcionamento. Encerrando execução.");
                    break;
                }

                processarLead(lead);

                if (!shouldStop.get()) {
                    aguardarIntervalo();
                }
            }
        } finally {
            isRunning.set(false);
            shouldStop.set(false);
            log.info("=== Prospecção de Contadores FINALIZADA ===");
        }
    }

    // -------------------------------------------------------------------------
    // Processamento individual de um lead
    // -------------------------------------------------------------------------

    private void processarLead(ProspectingDataSource lead) {
        log.info("Processando lead: CNPJ={}", lead.getCnpj());

        Optional<String> telefoneValidoOpt = phoneValidationService.validarTelefone(
                lead.getTelefone1(), lead.getTelefone2());

        if (telefoneValidoOpt.isEmpty()) {
            // Nenhum telefone válido
            lead.setStatus(STATUS_NENHUM_TELEFONE);
            dataSourceRepository.save(lead);
            log.info("CNPJ {}: nenhum telefone válido.", lead.getCnpj());
            return;
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
        enviarMensagem(lead.getCnpj(), telefoneValido, processed);
    }

    private void enviarMensagem(String cnpj, String telefoneValido, ProspectingProcessed processed) {
        ProspectingAudit audit = new ProspectingAudit();
        audit.setCnpj(cnpj);

        try {
            String mensagem = messageService.sortearMensagem();
            zApiClient.sendTextMessage(telefoneValido, mensagem);

            // Sucesso: atualizar registro processado
            processed.setDataContato(LocalDateTime.now());
            processed.setStatus(STATUS_CONTATO_INICIAL);
            processedRepository.save(processed);

            audit.setStatus(AUDIT_OK);
            log.info("CNPJ {}: mensagem enviada com sucesso para {}.", cnpj, telefoneValido);

        } catch (Exception ex) {
            audit.setStatus(AUDIT_ERROR);
            audit.setLog(ex.getMessage());
            log.error("CNPJ {}: falha ao enviar mensagem. Erro: {}", cnpj, ex.getMessage(), ex);
        } finally {
            auditRepository.save(audit);
        }
    }

    // -------------------------------------------------------------------------
    // Controle de horário e intervalo
    // -------------------------------------------------------------------------

    private boolean dentroDoHorario() {
        LocalDateTime agora = LocalDateTime.now();
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
        int minutos = intervalMin + random.nextInt(intervalMax - intervalMin + 1);
        log.info("Aguardando {} minuto(s) antes do próximo registro...", minutos);
        try {
            Thread.sleep((long) minutos * 60 * 1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Espera interrompida: {}", ex.getMessage());
        }
    }
}
