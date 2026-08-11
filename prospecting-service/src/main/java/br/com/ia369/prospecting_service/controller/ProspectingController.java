package br.com.ia369.prospecting_service.controller;

import br.com.ia369.prospecting_service.googlesheets.GoogleSheetsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@RestController
@RequestMapping("/prospecting")
@CrossOrigin(origins = "*") // Permite requisições de qualquer origem
public class ProspectingController {

    private final GoogleSheetsService googleSheetsService;

    public ProspectingController(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @GetMapping("/read-file")
    public ResponseEntity<List<List<Object>>> readFile() {
        try {
            List<List<Object>> data = googleSheetsService.readSheet();
            if (data == null || data.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(data);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
