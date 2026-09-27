package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.config.AssemblyAiProperties;
import com.assemblyai.api.AssemblyAI;
import com.assemblyai.api.resources.transcripts.types.Transcript;
import com.assemblyai.api.resources.transcripts.types.TranscriptLanguageCode;
import com.assemblyai.api.resources.transcripts.types.TranscriptOptionalParams;
import com.assemblyai.api.resources.transcripts.types.TranscriptStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;

@Service
public class AudioTranscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(AudioTranscriptionService.class);

    private final AssemblyAI assemblyAI;
    private final AssemblyAiProperties properties;
    private final HttpClient httpClient;

    @Autowired
    public AudioTranscriptionService(AssemblyAiProperties properties,
                                     Optional<AssemblyAI> assemblyAIOptional) {
        this(assemblyAIOptional.orElse(null), properties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build());
    }

    public AudioTranscriptionService(@Nullable AssemblyAI assemblyAI,
                                     AssemblyAiProperties properties) {
        this(assemblyAI, properties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build());
    }

    public AudioTranscriptionService(@Nullable AssemblyAI assemblyAI,
                                     AssemblyAiProperties properties,
                                     HttpClient httpClient) {
        this.assemblyAI = assemblyAI;
        this.properties = properties;
        this.httpClient = httpClient;
    }

    /**
     * Efetua o download do áudio e realiza a transcrição com AssemblyAI.
     * Caso a 1ª tentativa falhe, aguarda 10 segundos e tenta uma 2ª vez.
     * Se falhar novamente, retorna Optional.empty().
     */
    public Optional<String> transcribeAudioWithRetry(String audioUrl, String phone) {
        if (assemblyAI == null) {
            logger.error("❌ AssemblyAI não configurada (chave ausente). Impossível transcrever áudio para {}", phone);
            return Optional.empty();
        }

        Path tempAudioFile = null;
        try {
            tempAudioFile = downloadAudioFile(audioUrl, phone);
            if (tempAudioFile == null || !Files.exists(tempAudioFile)) {
                logger.error("❌ Falha no download do áudio ({}) para {}", audioUrl, phone);
                return Optional.empty();
            }

            // 1ª Tentativa de transcrição
            logger.info("⏳ [phone={}] Iniciando 1ª tentativa de transcrição com AssemblyAI...", phone);
            try {
                Optional<String> result = executeTranscription(tempAudioFile.toFile());
                if (result.isPresent() && !result.get().isBlank()) {
                    logger.info("✅ [phone={}] 1ª tentativa de transcrição concluída com sucesso.", phone);
                    return result;
                }
                logger.warn("⚠️ [phone={}] 1ª tentativa não retornou texto válido.", phone);
            } catch (Exception e) {
                logger.warn("⚠️ [phone={}] 1ª tentativa de transcrição falhou com erro: {}", phone, e.getMessage());
            }

            // Aguarda 10 segundos antes da 2ª tentativa
            long delay = properties.retryDelayMs() != null ? properties.retryDelayMs() : 10000L;
            logger.info("⏸️ [phone={}] Aguardando {} ms para realizar a 2ª tentativa de transcrição...", phone, delay);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.error("❌ [phone={}] Thread interrompida durante o intervalo de retry.", phone);
                return Optional.empty();
            }

            // 2ª Tentativa de transcrição
            logger.info("⏳ [phone={}] Iniciando 2ª tentativa de transcrição com AssemblyAI...", phone);
            try {
                Optional<String> result2 = executeTranscription(tempAudioFile.toFile());
                if (result2.isPresent() && !result2.get().isBlank()) {
                    logger.info("✅ [phone={}] 2ª tentativa de transcrição concluída com sucesso.", phone);
                    return result2;
                }
                logger.error("❌ [phone={}] 2ª tentativa de transcrição não retornou texto válido.", phone);
            } catch (Exception e) {
                logger.error("❌ [phone={}] 2ª tentativa de transcrição falhou com erro: {}", phone, e.getMessage());
            }

            logger.error("❌ [phone={}] Todas as tentativas de transcrição falharam.", phone);
            return Optional.empty();

        } catch (Exception e) {
            logger.error("❌ [phone={}] Erro inesperado durante o processo de transcrição: {}", phone, e.getMessage(), e);
            return Optional.empty();
        } finally {
            if (tempAudioFile != null) {
                try {
                    Files.deleteIfExists(tempAudioFile);
                    logger.debug("🧹 Arquivo temporário de áudio removido: {}", tempAudioFile);
                } catch (Exception ignored) {}
            }
        }
    }

    protected Optional<String> executeTranscription(File file) throws Exception {
        TranscriptOptionalParams params = TranscriptOptionalParams.builder()
                .languageCode(TranscriptLanguageCode.PT)
                .punctuate(true)
                .formatText(true)
                .build();

        Transcript transcript = assemblyAI.transcripts().transcribe(file, params);

        if (transcript.getStatus().equals(TranscriptStatus.COMPLETED)) {
            String text = transcript.getText().orElse("");
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } else if (transcript.getStatus().equals(TranscriptStatus.ERROR)) {
            logger.error("❌ AssemblyAI retornou status de erro: {}", transcript.getError().orElse("Erro desconhecido"));
            return Optional.empty();
        } else {
            logger.warn("⚠️ AssemblyAI retornou status: {}", transcript.getStatus());
            return Optional.empty();
        }
    }

    protected Path downloadAudioFile(String url, String phone) throws Exception {
        Path tempFile = Files.createTempFile("zapi_audio_" + phone + "_", ".ogg");

        if (url.startsWith("http://") || url.startsWith("https://")) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<Path> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofFile(tempFile));
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return tempFile;
            } else {
                logger.error("❌ Falha HTTP ({}) ao baixar áudio da URL: {}", resp.statusCode(), url);
                Files.deleteIfExists(tempFile);
                return null;
            }
        } else {
            Path localSource = url.startsWith("file:/") ? Paths.get(URI.create(url)) : Paths.get(url);
            if (!Files.exists(localSource)) {
                logger.error("❌ Arquivo local não encontrado: {}", localSource.toAbsolutePath());
                Files.deleteIfExists(tempFile);
                return null;
            }
            Files.copy(localSource, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        }
    }
}
