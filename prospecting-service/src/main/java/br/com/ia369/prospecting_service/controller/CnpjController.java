package br.com.ia369.prospecting_service.controller;

import br.com.ia369.prospecting_service.dto.CNPJResponse;
import br.com.ia369.prospecting_service.service.CnpjService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cnpj")
public class CnpjController {

    private static final Logger log = LoggerFactory.getLogger(CnpjController.class);

    private final CnpjService cnpjService;

    public CnpjController(CnpjService cnpjService) {
        this.cnpjService = cnpjService;
    }

    @GetMapping("/{cnpj}")
    public ResponseEntity<CNPJResponse> buscarPorCnpj(@PathVariable String cnpj) {
        log.info("Recebida requisicao para consultar CNPJ: {}", cnpj);
        return cnpjService.buscarPorCnpj(cnpj)
                .map(response -> {
                    log.info("CNPJ {} encontrado: {}", cnpj, response.razaoSocial());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("CNPJ {} nao encontrado", cnpj);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping("/processar-lote")
    public ResponseEntity<Void> processarLote() {
        log.info("Recebida requisicao para iniciar processamento em lote de CNPJs.");
        cnpjService.processarLoteDeCnpjs();
        return ResponseEntity.accepted().build();
    }
}
