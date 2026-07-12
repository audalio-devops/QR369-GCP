package br.com.ia369.prospecting_service.repository;

import br.com.ia369.prospecting_service.entity.BuscaCnpj;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuscaCnpjRepository extends JpaRepository<BuscaCnpj, Long> {
}
