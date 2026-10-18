package br.com.ia369.prospecting_service.repository;

import br.com.ia369.prospecting_service.model.ProspectingDataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProspectingDataSourceRepository extends JpaRepository<ProspectingDataSource, Long> {

    /**
     * Retorna todos os leads que ainda não foram processados (status nulo),
     * ordenados por prioridade ascendente.
     */
    List<ProspectingDataSource> findByStatusIsNullOrderByPrioridadeAsc();

    /**
     * Retorna leads pendentes filtrados por segmento.
     * Caso o segmento seja 'CONTADORES', inclui registros com segmento nulo para compatibilidade retroativa.
     */
    @org.springframework.data.jpa.repository.Query("SELECT p FROM ProspectingDataSource p WHERE (p.segmento = :segmento OR (:segmento = 'CONTADORES' AND p.segmento IS NULL)) AND p.status IS NULL ORDER BY p.prioridade ASC")
    List<ProspectingDataSource> findPendentesPorSegmento(@org.springframework.data.repository.query.Param("segmento") String segmento);

    /**
     * Retorna todos os leads que ainda não foram processados (status nulo).
     * @deprecated Utilizar {@link #findByStatusIsNullOrderByPrioridadeAsc()} para garantir ordenação por prioridade.
     */
    @Deprecated
    List<ProspectingDataSource> findByStatusIsNull();
}
