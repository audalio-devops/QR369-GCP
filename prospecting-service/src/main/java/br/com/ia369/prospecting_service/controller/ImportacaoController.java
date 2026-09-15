package br.com.ia369.prospecting_service.controller;

import br.com.ia369.prospecting_service.dto.ImportacaoResponse;
import br.com.ia369.prospecting_service.service.ImportacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prospectaccount")
public class ImportacaoController {

    @Autowired
    private ImportacaoService importacaoService;

    @PostMapping("/{type}")
    public ResponseEntity<ImportacaoResponse> importarDados(@PathVariable("type") int type) {
        if (type != 1 && type != 2) {
            return ResponseEntity.badRequest().body(new ImportacaoResponse("Erro", 0, "Tipo de importação inválido. Use 1 ou 2."));
        }
        ImportacaoResponse response = importacaoService.processarArquivo(type);
        return ResponseEntity.ok(response);
    }
}
