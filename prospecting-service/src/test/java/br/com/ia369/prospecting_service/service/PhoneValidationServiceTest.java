package br.com.ia369.prospecting_service.service;

import br.com.ia369.prospecting_service.client.ZApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneValidationServiceTest {

    @Mock
    private ZApiClient zApiClient;

    private PhoneValidationService service;

    @BeforeEach
    void setUp() {
        service = new PhoneValidationService(zApiClient);
    }

    @Test
    @DisplayName("Deve normalizar números nos mais diversos formatos")
    void testNormalizarTelefone() {
        assertEquals("5511999998888", service.normalizarTelefone("11999998888"));
        assertEquals("5511999998888", service.normalizarTelefone("(11) 99999-8888"));
        assertEquals("5511999998888", service.normalizarTelefone("+55 11 99999-8888"));
        assertEquals("5511999998888", service.normalizarTelefone("011999998888"));
        assertEquals("551133334444", service.normalizarTelefone("1133334444"));
        assertEquals("551133334444", service.normalizarTelefone("(11) 3333-4444"));
        assertEquals("551133334444", service.normalizarTelefone("551133334444"));
        assertEquals("", service.normalizarTelefone(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "5511999998888", // SP - Celular
            "5521988887777", // RJ - Celular
            "5531991112222", // MG - Celular
            "5541999991111", // PR - Celular
            "5551999992222", // RS - Celular
            "5561999993333", // DF - Celular
            "5571999994444", // BA - Celular
            "5585999995555", // CE - Celular
            "5591999996666"  // PA - Celular
    })
    @DisplayName("Deve validar números de celular válidos do Brasil (13 dígitos com DDI 55)")
    void testIsTelefoneValidoBrasilCelularValido(String numeroNormalizado) {
        assertTrue(service.isTelefoneValidoBrasil(numeroNormalizado),
                "Número de celular deveria ser considerado válido: " + numeroNormalizado);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "551133334444", // SP - Fixo (inicia com 3)
            "552125551234", // RJ - Fixo (inicia com 2)
            "553140041234", // MG - Fixo (inicia com 4)
            "554855551234"  // SC - Fixo (inicia com 5)
    })
    @DisplayName("Deve validar números de telefone fixo válidos do Brasil (12 dígitos com DDI 55)")
    void testIsTelefoneValidoBrasilFixoValido(String numeroNormalizado) {
        assertTrue(service.isTelefoneValidoBrasil(numeroNormalizado),
                "Número fixo deveria ser considerado válido: " + numeroNormalizado);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "5500999998888", // DDD 00 inexistente
            "5520999998888", // DDD 20 inexistente
            "5530999998888", // DDD 30 inexistente
            "5550999998888", // DDD 50 inexistente
            "5570999998888", // DDD 70 inexistente
            "5511899998888", // Celular com 13 dígitos mas não iniciando com 9 (inicia com 8)
            "551113334444",  // Fixo com 12 dígitos mas iniciando com 1
            "551163334444",  // Fixo com 12 dígitos mas iniciando com 6
            "55119999888",   // Tamanho insuficiente (11 dígitos com 55)
            "55692060111",   // Direcionado para o Chile
            "55119999988889" // Tamanho excedente (14 dígitos)
    })
    @DisplayName("Deve rejeitar números com DDD inválido, tamanho incorreto ou prefixos inexistentes")
    void testIsTelefoneValidoBrasilNumerosInvalidos(String numeroNormalizado) {
        assertFalse(service.isTelefoneValidoBrasil(numeroNormalizado),
                "Número deveria ser considerado inválido: " + numeroNormalizado);
    }

    @Test
    @DisplayName("Deve retornar telefone1 se for válido no BR e existir no WhatsApp")
    void testValidarTelefoneSucessoTel1() {
        when(zApiClient.phoneExists("5511999998888")).thenReturn(true);

        Optional<String> resultado = service.validarTelefone("(11) 99999-8888", "(11) 3333-4444");

        assertTrue(resultado.isPresent());
        assertEquals("5511999998888", resultado.get());
        verify(zApiClient).phoneExists("5511999998888");
        verify(zApiClient, never()).phoneExists("551133334444");
    }

    @Test
    @DisplayName("Deve pular chamada à Z-API para telefone1 inválido e validar telefone2")
    void testValidarTelefonePulandoTel1Invalido() {
        when(zApiClient.phoneExists("5511999998888")).thenReturn(true);

        // telefone1 é um número sem DDD válido ("11111111"), telefone2 é celular válido
        Optional<String> resultado = service.validarTelefone("11111111", "11999998888");

        assertTrue(resultado.isPresent());
        assertEquals("5511999998888", resultado.get());
        // Garante que não chamou a Z-API para o número1 inválido
        verify(zApiClient, never()).phoneExists(argThat(s -> s.contains("11111111")));
        verify(zApiClient).phoneExists("5511999998888");
    }

    @Test
    @DisplayName("Deve retornar Optional.empty se ambos os telefones forem inválidos no formato BR")
    void testValidarTelefoneAmbosInvalidos() {
        Optional<String> resultado = service.validarTelefone("1234", "00000000");

        assertFalse(resultado.isPresent());
        verify(zApiClient, never()).phoneExists(anyString());
    }
}
