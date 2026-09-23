package br.com.ia369.prospecting_service.service;

import br.com.ia369.prospecting_service.client.ZApiClient;
import br.com.ia369.prospecting_service.exception.ZApiDisconnectedException;
import br.com.ia369.prospecting_service.repository.ProspectingProcessedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneValidationServiceTest {

    @Mock
    private ZApiClient zApiClient;

    @Mock
    private ProspectingProcessedRepository processedRepository;

    private PhoneValidationService service;

    @BeforeEach
    void setUp() {
        service = new PhoneValidationService(zApiClient, processedRepository);
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
    @DisplayName("Deve lançar ZApiDisconnectedException quando Z-API estiver desconectada no telefone1")
    void testValidarTelefoneLancaZApiDisconnectedExceptionTel1() {
        when(zApiClient.isConnected()).thenReturn(false);

        ZApiDisconnectedException ex = assertThrows(ZApiDisconnectedException.class, () ->
                service.validarTelefone("(11) 99999-8888", "(11) 3333-4444")
        );

        assertEquals("Erro: Instância Web Z-API desconectada", ex.getMessage());
        verify(zApiClient).isConnected();
        verify(zApiClient, never()).phoneExists(anyString());
    }

    @Test
    @DisplayName("Deve lançar ZApiDisconnectedException antes de validar telefone2 se telefone1 for inválido")
    void testValidarTelefoneLancaZApiDisconnectedExceptionTel2() {
        when(zApiClient.isConnected()).thenReturn(false);

        // telefone1 inválido no formato ("11111111"), telefone2 celular válido
        ZApiDisconnectedException ex = assertThrows(ZApiDisconnectedException.class, () ->
                service.validarTelefone("11111111", "11999998888")
        );

        assertEquals("Erro: Instância Web Z-API desconectada", ex.getMessage());
        verify(zApiClient).isConnected();
        verify(zApiClient, never()).phoneExists(anyString());
    }

    @Test
    @DisplayName("Deve retornar telefone1 se for válido no BR e existir no WhatsApp")
    void testValidarTelefoneSucessoTel1() {
        when(zApiClient.isConnected()).thenReturn(true);
        when(zApiClient.phoneExists("5511999998888")).thenReturn(true);

        PhoneValidationService.ResultadoValidacaoTelefone resultado = service.validarTelefone("(11) 99999-8888", "(11) 3333-4444");

        assertTrue(resultado.aptoParaContato());
        assertEquals("5511999998888", resultado.telefone());
        verify(zApiClient).isConnected();
        verify(processedRepository).existsByTelefoneValido("5511999998888");
        verify(zApiClient).phoneExists("5511999998888");
        verify(zApiClient, never()).phoneExists("551133334444");
    }

    @Test
    @DisplayName("Deve pular chamada à Z-API para telefone1 inválido e validar telefone2")
    void testValidarTelefonePulandoTel1Invalido() {
        when(zApiClient.isConnected()).thenReturn(true);
        when(zApiClient.phoneExists("5511999998888")).thenReturn(true);

        // telefone1 é um número sem DDD válido ("11111111"), telefone2 é celular válido
        PhoneValidationService.ResultadoValidacaoTelefone resultado = service.validarTelefone("11111111", "11999998888");

        assertTrue(resultado.aptoParaContato());
        assertEquals("5511999998888", resultado.telefone());
        verify(zApiClient).isConnected();
        // Garante que não chamou a Z-API para o número1 inválido
        verify(zApiClient, never()).phoneExists(argThat(s -> s.contains("11111111")));
        verify(zApiClient).phoneExists("5511999998888");
    }

    @Test
    @DisplayName("Deve informar ausência de telefone válido se ambos forem inválidos no formato BR")
    void testValidarTelefoneAmbosInvalidos() {
        PhoneValidationService.ResultadoValidacaoTelefone resultado = service.validarTelefone("1234", "00000000");

        assertEquals(PhoneValidationService.ResultadoValidacaoTelefone.Status.NENHUM_TELEFONE_VALIDO,
                resultado.status());
        verify(zApiClient, never()).isConnected();
        verify(zApiClient, never()).phoneExists(anyString());
    }

    @Test
    @DisplayName("Deve informar explicitamente quando o telefone válido já foi contactado")
    void testValidarTelefoneJaContactado() {
        when(processedRepository.existsByTelefoneValido("5511999998888")).thenReturn(true);

        PhoneValidationService.ResultadoValidacaoTelefone resultado =
                service.validarTelefone("(11) 99999-8888", "(11) 3333-4444");

        assertTrue(resultado.jaContactado());
        assertEquals("5511999998888", resultado.telefone());
        verify(processedRepository).existsByTelefoneValido("5511999998888");
        verify(zApiClient, never()).isConnected();
        verify(zApiClient, never()).phoneExists(anyString());
    }

    @Test
    @DisplayName("Deve delegar isZApiConnected diretamente para o cliente Z-API")
    void testIsZApiConnected() {
        when(zApiClient.isConnected()).thenReturn(true);
        assertTrue(service.isZApiConnected());
        verify(zApiClient).isConnected();
    }

    @Test
    @DisplayName("Deve executar callback onZApiConnected quando a Z-API estiver conectada antes de phoneExists")
    void testValidarTelefoneInvocaCallbackOnZApiConnected() {
        when(zApiClient.isConnected()).thenReturn(true);
        when(zApiClient.phoneExists("5511999998888")).thenReturn(true);

        Runnable callbackMock = mock(Runnable.class);

        PhoneValidationService.ResultadoValidacaoTelefone resultado =
                service.validarTelefone("(11) 99999-8888", null, callbackMock);

        assertTrue(resultado.aptoParaContato());
        verify(zApiClient).isConnected();
        verify(callbackMock).run();
        verify(zApiClient).phoneExists("5511999998888");
    }
}
