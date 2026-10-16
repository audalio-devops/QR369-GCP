package br.com.ia369.virtual_assistant.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);

    private final IngestionService ingestionService;

    @Value("${app.rag.default-doc-resource:classpath:/docs/Treinamento_Geral_Assistente.pdf}")
    private Resource defaultDocResource;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Integer> ingest(@RequestParam("file") MultipartFile file) {
        return Map.of("chunksStored", ingestionService.ingest(file.getResource()));
    }

    @PostMapping("/ingest/default")
    public ResponseEntity<Map<String, Object>> ingestDefault() {
        if (defaultDocResource == null || !defaultDocResource.exists()) {
            log.warn("Documento padrão não encontrado em: {}", defaultDocResource != null ? defaultDocResource.getDescription() : "null");
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Documento padrão não encontrado em: " + (defaultDocResource != null ? defaultDocResource.getDescription() : "null")
            ));
        }

        int chunks = ingestionService.ingest(defaultDocResource);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "resource", defaultDocResource.getFilename(),
                "chunksStored", chunks
        ));
    }
}