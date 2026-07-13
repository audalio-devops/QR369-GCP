package br.com.ia369.prospecting_service.service;

import br.com.ia369.prospecting_service.client.BrasilApiClient;
import br.com.ia369.prospecting_service.dto.CNPJResponse;
import br.com.ia369.prospecting_service.entity.BuscaCnpj;
import br.com.ia369.prospecting_service.entity.Empresa;
import br.com.ia369.prospecting_service.mapper.EmpresaMapper;
import br.com.ia369.prospecting_service.repository.BuscaCnpjRepository;
import br.com.ia369.prospecting_service.repository.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CnpjService {

    private static final Logger log = LoggerFactory.getLogger(CnpjService.class);

    private final EmpresaRepository empresaRepository;
    private final BuscaCnpjRepository buscaCnpjRepository;
    private final BrasilApiClient brasilApiClient;

    public CnpjService(EmpresaRepository empresaRepository,
            BuscaCnpjRepository buscaCnpjRepository,
            BrasilApiClient brasilApiClient) {
        this.empresaRepository = empresaRepository;
        this.buscaCnpjRepository = buscaCnpjRepository;
        this.brasilApiClient = brasilApiClient;
    }

    public Optional<CNPJResponse> buscarPorCnpj(String cnpj) {
        log.info("Buscando CNPJ no cache local (banco): {}", cnpj);
        Optional<Empresa> empresaOpt = empresaRepository.findById(cnpj);
        if (empresaOpt.isPresent()) {
            log.info("CNPJ {} encontrado no cache local.", cnpj);
            return empresaOpt.map(this::toResponse);
        }

        log.info("CNPJ {} não encontrado localmente. Consultando BrasilAPI...", cnpj);
        return brasilApiClient.buscarCnpj(cnpj)
                .map(dto -> {
                    Empresa empresa = EmpresaMapper.toEntity(dto);
                    Empresa salva = empresaRepository.save(empresa);
                    log.info("CNPJ {} obtido da BrasilAPI e salvo localmente.", cnpj);
                    return toResponse(salva);
                });
    }

    @Async
    public void processarLoteDeCnpjs() {
        log.info("Iniciando processamento em lote de CNPJs...");
        List<BuscaCnpj> fila = buscaCnpjRepository.findAll();
        log.info("Encontrados {} CNPJs na fila para processar.", fila.size());

        for (BuscaCnpj item : fila) {
            try {
                Optional<CNPJResponse> resultado = buscarPorCnpj(item.getCnpj());
                if (resultado.isPresent()) {
                    log.info("CNPJ {} processado com sucesso: {}",
                            item.getCnpj(), resultado.get().razaoSocial());
                } else {
                    log.warn("CNPJ {} nao encontrado na base de dados nem na BrasilAPI.", item.getCnpj());
                }
            } catch (Exception e) {
                log.error("Falha ao processar o CNPJ {} do lote: {}",
                        item.getCnpj(), e.getMessage(), e);
            }
        }

        log.info("Processamento em lote finalizado. {} CNPJs processados.", fila.size());
    }

    private CNPJResponse toResponse(Empresa e) {
        return new CNPJResponse(
                e.getCnpj(),
                e.getRazaoSocial(),
                e.getNomeFantasia(),
                e.getSituacaoCadastral(),
                e.getDataAbertura(),
                e.getDdd(),
                e.getTelefone(),
                e.getEmail(),
                e.getLogradouro(),
                e.getNumero(),
                e.getComplemento(),
                e.getBairro(),
                e.getCidade(),
                e.getUf(),
                e.getCep());
    }
}
