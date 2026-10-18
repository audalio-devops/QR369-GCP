package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.config.AssemblyAiProperties;
import com.assemblyai.api.AssemblyAI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AudioTranscriptionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnEmptyWhenAssemblyAiIsNull() {
        var properties = new AssemblyAiProperties("fake-key", "pt", 10L);
        var service = new AudioTranscriptionService(null, properties);

        Optional<String> result = service.transcribeAudioWithRetry("https://example.com/audio.ogg", "5511999999999");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldSucceedOnFirstAttempt() throws IOException {
        Path dummyAudio = Files.createTempFile(tempDir, "test", ".ogg");
        var properties = new AssemblyAiProperties("fake-key", "pt", 10L);
        var mockAai = mock(AssemblyAI.class);

        var service = new AudioTranscriptionService(mockAai, properties) {
            @Override
            protected Path downloadAudioFile(String url, String phone) {
                return dummyAudio;
            }

            @Override
            protected Optional<String> executeTranscription(File file) {
                return Optional.of("Transcrição realizada na 1ª tentativa");
            }
        };

        Optional<String> result = service.transcribeAudioWithRetry("https://example.com/audio.ogg", "5511999999999");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("Transcrição realizada na 1ª tentativa");
    }

    @Test
    void shouldRetryAndSucceedOnSecondAttemptAfterFirstFails() throws IOException {
        Path dummyAudio = Files.createTempFile(tempDir, "test", ".ogg");
        // delay curto (10ms) para o teste rodar rápido
        var properties = new AssemblyAiProperties("fake-key", "pt", 10L);
        var mockAai = mock(AssemblyAI.class);
        var attemptCounter = new AtomicInteger(0);

        var service = new AudioTranscriptionService(mockAai, properties) {
            @Override
            protected Path downloadAudioFile(String url, String phone) {
                return dummyAudio;
            }

            @Override
            protected Optional<String> executeTranscription(File file) {
                int attempt = attemptCounter.incrementAndGet();
                if (attempt == 1) {
                    throw new RuntimeException("Erro transitório na 1ª tentativa");
                }
                return Optional.of("Transcrição realizada na 2ª tentativa com sucesso");
            }
        };

        Optional<String> result = service.transcribeAudioWithRetry("https://example.com/audio.ogg", "5511999999999");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("Transcrição realizada na 2ª tentativa com sucesso");
        assertThat(attemptCounter.get()).isEqualTo(2);
    }

    @Test
    void shouldFailAfterBothAttemptsFail() throws IOException {
        Path dummyAudio = Files.createTempFile(tempDir, "test", ".ogg");
        var properties = new AssemblyAiProperties("fake-key", "pt", 10L);
        var mockAai = mock(AssemblyAI.class);
        var attemptCounter = new AtomicInteger(0);

        var service = new AudioTranscriptionService(mockAai, properties) {
            @Override
            protected Path downloadAudioFile(String url, String phone) {
                return dummyAudio;
            }

            @Override
            protected Optional<String> executeTranscription(File file) {
                attemptCounter.incrementAndGet();
                return Optional.empty(); // Ambas falham sem texto
            }
        };

        Optional<String> result = service.transcribeAudioWithRetry("https://example.com/audio.ogg", "5511999999999");

        assertThat(result).isEmpty();
        assertThat(attemptCounter.get()).isEqualTo(2);
    }
}
