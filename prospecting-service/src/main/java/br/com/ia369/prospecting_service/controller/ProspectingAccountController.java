package br.com.ia369.prospecting_service.controller;

import br.com.ia369.prospecting_service.service.ProspectingAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para o endpoint de prospecção de contadores.
 *
 * POST /prospecting-account → inicia o processamento (async, retorna 202)
 * DELETE /prospecting-account → sinaliza parada do processamento
 * GET /prospecting-account/status → retorna se está em execução
 */
@RestController
@RequestMapping("/prospecting-account")
public class ProspectingAccountController {

    private static final Logger log = LoggerFactory.getLogger(ProspectingAccountController.class);

    private final ProspectingAccountService prospectingAccountService;

    public ProspectingAccountController(ProspectingAccountService prospectingAccountService) {
        this.prospectingAccountService = prospectingAccountService;
    }

    /**
     * Inicia a prospecção de contadores de forma assíncrona.
     *
     * @return 202 Accepted se iniciado com sucesso, 409 Conflict se já estiver em
     *         execução
     */
    @PostMapping
    public ResponseEntity<String> startProspecting() {
        if (prospectingAccountService.isRunning()) {
            log.warn("Tentativa de iniciar prospecção já em andamento.");
            return ResponseEntity.status(409).body("Prospecção já em andamento.");
        }
        prospectingAccountService.startProspecting();
        log.info("Prospecção de contadores iniciada via endpoint.");
        return ResponseEntity.accepted().body("Prospecção de contadores iniciada.");
    }

    /**
     * Sinaliza a parada do processamento em andamento.
     *
     * @return 200 OK com mensagem de confirmação
     */
    @DeleteMapping
    public ResponseEntity<String> stopProspecting() {
        if (!prospectingAccountService.isRunning()) {
            return ResponseEntity.ok("Prospecção não está em execução.");
        }
        prospectingAccountService.stop();
        return ResponseEntity.ok("Sinal de parada enviado. A prospecção será encerrada após o registro atual.");
    }

    /**
     * Retorna o status atual de execução.
     *
     * @return 200 OK com JSON simples indicando se está em execução
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        boolean running = prospectingAccountService.isRunning();
        return ResponseEntity.ok("{\"running\": " + running + "}");
    }
}
