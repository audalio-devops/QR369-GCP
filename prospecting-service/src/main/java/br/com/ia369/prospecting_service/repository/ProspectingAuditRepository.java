package br.com.ia369.prospecting_service.repository;

import br.com.ia369.prospecting_service.model.ProspectingAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProspectingAuditRepository extends JpaRepository<ProspectingAudit, Long> {

    /**
     * Conta registros de auditoria após o instante informado — usado pelo script de
     * monitoramento via API.
     */
    long countByDataEnvioAfter(LocalDateTime after);
}
