package br.com.ia369.prospecting_service.service;

import br.com.ia369.prospecting_service.client.ZApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Serviço responsável por validar se telefone1 ou telefone2 de um lead
 * possui cadastro no WhatsApp, usando a Z-API.
 *
 * Lógica:
 * 1. Se telefone1 não for nulo/vazio → verifica via Z-API
 * - Se válido: retorna telefone1
 * 2. Se telefone2 não for nulo/vazio → verifica via Z-API
 * - Se válido: retorna telefone2
 * 3. Se nenhum for válido: retorna Optional.empty()
 */
@Service
public class PhoneValidationService {

    private static final Logger log = LoggerFactory.getLogger(PhoneValidationService.class);

    private final ZApiClient zApiClient;

    public PhoneValidationService(ZApiClient zApiClient) {
        this.zApiClient = zApiClient;
    }

    /**
     * Valida telefone1 e/ou telefone2 no WhatsApp.
     *
     * @param telefone1 primeiro telefone (pode ser nulo)
     * @param telefone2 segundo telefone (pode ser nulo)
     * @return Optional com o primeiro telefone válido encontrado, ou empty se
     *         nenhum for válido
     */
    public Optional<String> validarTelefone(String telefone1, String telefone2) {
        if (hasValue(telefone1)) {
            log.info("Verificando telefone1: {}", telefone1);
            if (zApiClient.phoneExists(normalizarTelefone(telefone1))) {
                return Optional.of(normalizarTelefone(telefone1));
            }
        }

        if (hasValue(telefone2)) {
            log.info("Verificando telefone2: {}", telefone2);
            if (zApiClient.phoneExists(normalizarTelefone(telefone2))) {
                return Optional.of(normalizarTelefone(telefone2));
            }
        }

        log.warn("Nenhum telefone válido encontrado (tel1={}, tel2={})", telefone1, telefone2);
        return Optional.empty();
    }

    /**
     * Garante que o número esteja no formato DDI+DDD+número, apenas dígitos.
     * Exemplo: "11 99999-9999" → "5511999999999"
     */
    private String normalizarTelefone(String telefone) {
        String apenasDigitos = telefone.replaceAll("[^0-9]", "");
        // Se já tiver 13 dígitos (DDI+DDD+9+número) ou 12 dígitos (DDI+DDD+número),
        // retornar como está
        if (apenasDigitos.startsWith("55") && apenasDigitos.length() >= 12) {
            return apenasDigitos;
        }
        // Adicionar DDI do Brasil
        return "55" + apenasDigitos;
    }

    private boolean hasValue(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
