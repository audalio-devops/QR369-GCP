package br.com.ia369.prospecting_service.controller;

import br.com.ia369.prospecting_service.dto.CNPJResponse;
import br.com.ia369.prospecting_service.service.CnpjService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cnpj")
@RequiredArgsConstructor
public class CnpjController {

    private static final Logger log = LoggerFactory.getLogger(CnpjController.class);

    private final CnpjService cnpjService;

    @GetMapping("/{cnpj}")
    public ResponseEntity<CNPJResponse> findByCnpj(@PathVariable String cnpj) {
        log.info("Recebida requisição para consultar CNPJ: {}", cnpj);
        return cnpjService.findByCnpj(cnpj)
                .map(response -> {
                    log.info("CNPJ {} encontrado: {}", cnpj, response.razaoSocial());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("CNPJ {} não encontrado", cnpj);
                    return ResponseEntity.notFound().build();
                });
    }
}
