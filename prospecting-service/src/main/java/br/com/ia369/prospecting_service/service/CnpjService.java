package br.com.ia369.prospecting_service.service;

import br.com.ia369.prospecting_service.dto.CNPJResponse;
import br.com.ia369.prospecting_service.entity.Empresa;
import br.com.ia369.prospecting_service.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CnpjService {

    private final EmpresaRepository empresaRepository;

    public CnpjService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public Optional<CNPJResponse> buscarPorCnpj(String cnpj) {
        return empresaRepository.findById(cnpj).map(this::toResponse);
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
