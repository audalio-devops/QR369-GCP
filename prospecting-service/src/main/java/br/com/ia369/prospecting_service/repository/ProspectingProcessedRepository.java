package br.com.ia369.prospecting_service.repository;

import br.com.ia369.prospecting_service.model.ProspectingProcessed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProspectingProcessedRepository extends JpaRepository<ProspectingProcessed, Long> {

    Optional<ProspectingProcessed> findTopByCnpjOrderByCreatedAtDesc(String cnpj);
}
