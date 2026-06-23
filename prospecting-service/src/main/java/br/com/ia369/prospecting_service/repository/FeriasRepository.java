package br.com.ia369.prospecting_service.repository;

import br.com.ia369.prospecting_service.entity.Ferias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeriasRepository extends JpaRepository<Ferias, Long> {

	// Busca as ferias de um colaborador pela matricula.
	Optional<Ferias> findByMatricula(String matricula);
}
