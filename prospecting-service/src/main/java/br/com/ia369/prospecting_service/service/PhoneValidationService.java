package br.com.ia369.prospecting_service.service;

import br.com.ia369.prospecting_service.client.ZApiClient;
import br.com.ia369.prospecting_service.exception.ZApiDisconnectedException;
import br.com.ia369.prospecting_service.repository.ProspectingProcessedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Serviço responsável por validar se telefone1 ou telefone2 de um lead
 * é um número válido do Brasil (fixo ou celular) e possui cadastro no WhatsApp via Z-API.
 */
@Service
public class PhoneValidationService {

    private static final Logger log = LoggerFactory.getLogger(PhoneValidationService.class);

    /**
     * DDDs válidos no Brasil:
     * 11..19, 21, 22, 24, 27, 28, 31..35, 37, 38, 41..49, 51, 53..55, 61..69, 71, 73..75, 77, 79, 81..89, 91..99
     */
    private static final String REGEX_DDD_VALIDO = "(1[1-9]|2[12478]|3[1-578]|4[1-9]|5[1345]|6[1-9]|7[134579]|8[1-9]|9[1-9])";

    /**
     * Celular Brasil: 55 + DDD + 9 dígitos iniciando por 9 (Ex: 5511999998888) -> total 13 dígitos
     * Fixo Brasil: 55 + DDD + 8 dígitos iniciando por 2, 3, 4 ou 5 (Ex: 551133334444) -> total 12 dígitos
     */
    private static final Pattern PATTERN_TELEFONE_BR = Pattern.compile("^55" + REGEX_DDD_VALIDO + "(9[0-9]{8}|[2-5][0-9]{7})$");

    private final ZApiClient zApiClient;
    private final ProspectingProcessedRepository processedRepository;

    public PhoneValidationService(ZApiClient zApiClient, ProspectingProcessedRepository processedRepository) {
        this.zApiClient = zApiClient;
        this.processedRepository = processedRepository;
    }

    /**
     * Verifica se a Z-API está conectada ao WhatsApp.
     *
     * @return true se conectada, false caso contrário
     */
    public boolean isZApiConnected() {
        return zApiClient.isConnected();
    }

    /**
     * Valida se telefone1 ou telefone2 é um número válido do Brasil (fixo ou celular)
     * e se existe no WhatsApp.
     *
     * @param telefone1 primeiro telefone (pode ser nulo)
     * @param telefone2 segundo telefone (pode ser nulo)
     * @return resultado que diferencia telefone apto para contato, número já contactado e
     *         ausência de telefone válido
     * @throws ZApiDisconnectedException se a instância da Z-API estiver desconectada
     */
    public ResultadoValidacaoTelefone validarTelefone(String telefone1, String telefone2) {
        return validarTelefone(telefone1, telefone2, null);
    }

    /**
     * Valida se telefone1 ou telefone2 é um número válido do Brasil (fixo ou celular)
     * e se existe no WhatsApp, permitindo executar um callback de monitoramento quando
     * a conexão da Z-API for confirmada antes da verificação no WhatsApp.
     *
     * @param telefone1 primeiro telefone (pode ser nulo)
     * @param telefone2 segundo telefone (pode ser nulo)
     * @param onZApiConnected callback a ser invocado quando a conexão da Z-API for confirmada
     * @return resultado que diferencia telefone apto para contato, número já contactado e
     *         ausência de telefone válido
     * @throws ZApiDisconnectedException se a instância da Z-API estiver desconectada
     */
    public ResultadoValidacaoTelefone validarTelefone(String telefone1, String telefone2, Runnable onZApiConnected) {
        boolean zApiConexaoVerificada = false;

        if (hasValue(telefone1)) {
            String norm1 = normalizarTelefone(telefone1);
            log.info("Verificando telefone1: {} (normalizado: {})", telefone1, norm1);
            if (isTelefoneValidoBrasil(norm1)) {
                if (processedRepository.existsByTelefoneValido(norm1)) {
                    log.info("Telefone1 ja contactado: {}", norm1);
                    return ResultadoValidacaoTelefone.jaContactado(norm1);
                }
                if (!zApiConexaoVerificada) {
                    if (!zApiClient.isConnected()) {
                        throw new ZApiDisconnectedException("Erro: Instância Web Z-API desconectada");
                    }
                    zApiConexaoVerificada = true;
                    if (onZApiConnected != null) {
                        onZApiConnected.run();
                    }
                }
                if (zApiClient.phoneExists(norm1)) {
                    return ResultadoValidacaoTelefone.aptoParaContato(norm1);
                }
            } else {
                log.warn("telefone1 não é um número fixo ou celular válido do Brasil: {}", norm1);
            }
        }

        if (hasValue(telefone2)) {
            String norm2 = normalizarTelefone(telefone2);
            log.info("Verificando telefone2: {} (normalizado: {})", telefone2, norm2);
            if (isTelefoneValidoBrasil(norm2)) {
                if (processedRepository.existsByTelefoneValido(norm2)) {
                    log.info("Telefone2 ja contactado: {}", norm2);
                    return ResultadoValidacaoTelefone.jaContactado(norm2);
                }
                if (!zApiConexaoVerificada) {
                    if (!zApiClient.isConnected()) {
                        throw new ZApiDisconnectedException("Erro: Instância Web Z-API desconectada");
                    }
                    zApiConexaoVerificada = true;
                    if (onZApiConnected != null) {
                        onZApiConnected.run();
                    }
                }
                if (zApiClient.phoneExists(norm2)) {
                    return ResultadoValidacaoTelefone.aptoParaContato(norm2);
                }
            } else {
                log.warn("telefone2 não é um número fixo ou celular válido do Brasil: {}", norm2);
            }
        }

        log.warn("Nenhum telefone válido encontrado (tel1={}, tel2={})", telefone1, telefone2);
        return ResultadoValidacaoTelefone.nenhumTelefoneValido();
    }

    /**
     * Garante que o número esteja no formato DDI+DDD+número, apenas dígitos.
     * Trata zeros iniciais do DDD ou DDI (ex: "011999998888" -> "5511999998888").
     *
     * @param telefone número original bruto
     * @return telefone normalizado com prefixo DDI 55
     */
    public String normalizarTelefone(String telefone) {
        if (telefone == null) {
            return "";
        }
        String apenasDigitos = telefone.replaceAll("[^0-9]", "");
        apenasDigitos = apenasDigitos.replaceFirst("^0+", "");

        if (apenasDigitos.startsWith("55") && (apenasDigitos.length() == 12 || apenasDigitos.length() == 13)) {
            return apenasDigitos;
        }

        if (apenasDigitos.length() == 10 || apenasDigitos.length() == 11) {
            return "55" + apenasDigitos;
        }

        if (!apenasDigitos.startsWith("55")) {
            return "55" + apenasDigitos;
        }

        return apenasDigitos;
    }

    /**
     * Valida se um telefone normalizado (com DDI 55) é um celular ou fixo válido no Brasil.
     *
     * @param telefoneNormalizado número no formato 55 + DDD + número
     * @return true se for válido (celular ou fixo com DDD válido do Brasil), false caso contrário
     */
    public boolean isTelefoneValidoBrasil(String telefoneNormalizado) {
        if (telefoneNormalizado == null || telefoneNormalizado.isEmpty()) {
            return false;
        }
        return PATTERN_TELEFONE_BR.matcher(telefoneNormalizado).matches();
    }

    private boolean hasValue(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public record ResultadoValidacaoTelefone(Status status, String telefone) {

        public enum Status {
            APTO_PARA_CONTATO,
            JA_CONTACTADO,
            NENHUM_TELEFONE_VALIDO
        }

        public static ResultadoValidacaoTelefone aptoParaContato(String telefone) {
            return new ResultadoValidacaoTelefone(Status.APTO_PARA_CONTATO, telefone);
        }

        public static ResultadoValidacaoTelefone jaContactado(String telefone) {
            return new ResultadoValidacaoTelefone(Status.JA_CONTACTADO, telefone);
        }

        public static ResultadoValidacaoTelefone nenhumTelefoneValido() {
            return new ResultadoValidacaoTelefone(Status.NENHUM_TELEFONE_VALIDO, null);
        }

        public boolean aptoParaContato() {
            return status == Status.APTO_PARA_CONTATO;
        }

        public boolean jaContactado() {
            return status == Status.JA_CONTACTADO;
        }
    }
}
