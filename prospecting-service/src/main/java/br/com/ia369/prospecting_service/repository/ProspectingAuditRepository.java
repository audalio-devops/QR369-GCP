package br.com.ia369.prospecting_service.repository;

import br.com.ia369.prospecting_service.model.ProspectingAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProspectingAuditRepository extends JpaRepository<ProspectingAudit, Long> {

    /**
     * Conta registros de auditoria após o instante informado — usado pelo script de
     * monitoramento via API.
     */
    long countByDataEventoAfter(LocalDateTime after);

    /**
     * Retorna os 10 registros de auditoria mais recentes.
     */
    List<ProspectingAudit> findTop10ByOrderByDataEventoDesc();

    /**
     * Retorna registros de auditoria mais recentes com suporte a paginação/limite.
     */
    List<ProspectingAudit> findByOrderByDataEventoDesc(Pageable pageable);
}
