package br.com.ia369.prospecting_service.task;

import br.com.ia369.prospecting_service.service.CnpjService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarefa agendada que dispara o processamento em lote de CNPJs
 * de acordo com a expressao cron configurada em application.yml.
 * O processamento em si ocorre de forma assincrona no CnpjService.
 */
@Component
public class ScheduledProspectingTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledProspectingTask.class);

    private final CnpjService cnpjService;

    public ScheduledProspectingTask(CnpjService cnpjService) {
        this.cnpjService = cnpjService;
    }

    /**
     * Executa a prospeccao agendada conforme a cron definida em:
     * prospecting.schedule.cron (padrao: diariamente as 02:00).
     */
    @Scheduled(cron = "${prospecting.schedule.cron:0 0 2 * * ?}")
    public void executarProspeccaoAgendada() {
        log.info("Disparando tarefa agendada de prospeccao de CNPJs.");
        cnpjService.processarLoteDeCnpjs();
    }
}
