package br.com.ia369.prospecting_service.service;

import br.com.ia369.prospecting_service.dto.CNPJResponse;
import br.com.ia369.prospecting_service.entity.Cnpj;
import br.com.ia369.prospecting_service.repository.CnpjRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CnpjService {

    private final CnpjRepository cnpjRepository;

    public CnpjService(CnpjRepository cnpjRepository) {
        this.cnpjRepository = cnpjRepository;
    }

    public Optional<CNPJResponse> buscarPorCnpj(String cnpj) {
        return cnpjRepository.findById(cnpj).map(this::toResponse);
    }

    private CNPJResponse toResponse(Cnpj c) {
        return new CNPJResponse(
                c.getCnpj(),
                c.getRazaoSocial(),
                c.getNomeFantasia(),
                c.getSituacaoCadastral(),
                c.getDataAbertura(),
                c.getDdd(),
                c.getTelefone(),
                c.getEmail(),
                c.getLogradouro(),
                c.getNumero(),
                c.getComplemento(),
                c.getBairro(),
                c.getCidade(),
                c.getUf(),
                c.getCep());
    }
}
