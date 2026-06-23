package br.com.ia369.prospecting_service.controller;

import br.com.ia369.prospecting_service.dto.FeriasResponse;
import br.com.ia369.prospecting_service.repository.FeriasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ferias")
public class FeriasController {

	private static final Logger log = LoggerFactory.getLogger(FeriasController.class);

	private final FeriasRepository feriasRepository;

	public FeriasController(FeriasRepository feriasRepository) {
		this.feriasRepository = feriasRepository;
	}

	// Retorna as ferias do colaborador pela matricula. 200 se existir, 404 caso contrario.
	@GetMapping("/{matricula}")
	public ResponseEntity<FeriasResponse> buscarPorMatricula(@PathVariable String matricula) {
		return feriasRepository.findByMatricula(matricula)
				.map(ferias -> {
					log.info("Consulta de ferias para matricula {}: encontrada", matricula);
					return ResponseEntity.ok(FeriasResponse.from(ferias));
				})
				.orElseGet(() -> {
					log.info("Consulta de ferias para matricula {}: nao encontrada", matricula);
					return ResponseEntity.notFound().build();
				});
	}
}
