package br.com.ia369.prospecting_service.repository;

import br.com.ia369.prospecting_service.model.ProspectingDataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProspectingDataSourceRepository extends JpaRepository<ProspectingDataSource, Long> {

    /**
     * Retorna todos os leads que ainda não foram processados (status nulo).
     */
    List<ProspectingDataSource> findByStatusIsNull();
}
