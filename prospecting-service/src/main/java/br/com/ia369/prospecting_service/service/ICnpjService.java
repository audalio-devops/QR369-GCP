package br.com.ia369.prospecting_service.service;

import br.com.ia369.prospecting_service.dto.CNPJResponse;
import java.util.Optional;

public interface ICnpjService {
    Optional<CNPJResponse> findByCnpj(String cnpj);
}
