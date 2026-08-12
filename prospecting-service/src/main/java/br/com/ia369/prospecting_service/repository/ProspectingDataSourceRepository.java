package br.com.ia369.prospecting_service.repository;

import br.com.ia369.prospecting_service.model.ProspectingDataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProspectingDataSourceRepository extends JpaRepository<ProspectingDataSource, Long> {
}
