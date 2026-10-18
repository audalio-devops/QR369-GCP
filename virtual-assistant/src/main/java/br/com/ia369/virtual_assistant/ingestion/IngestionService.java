package br.com.ia369.virtual_assistant.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public int ingest(Resource resource) {
        log.info("Iniciando ingestão de documento para o VectorStore: {}", resource.getDescription());
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.read();
        log.info("Documentos extraídos pelo Tika: {}. Dividindo em chunks...", documents.size());
        List<Document> chunks = TokenTextSplitter.builder().build().apply(documents);
        log.info("Total de chunks gerados: {}. Gravando no VectorStore...", chunks.size());
        vectorStore.add(chunks);
        log.info("✅ Ingestão finalizada! {} chunks persistidos no VectorStore.", chunks.size());
        return chunks.size();
    }
}