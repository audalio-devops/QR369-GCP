package br.com.ia369.prospecting_service.config;

import br.com.ia369.prospecting_service.entity.Ferias;
import br.com.ia369.prospecting_service.repository.FeriasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

// Semeia a base H2 em memoria com colaboradores de exemplo no startup.
@Component
public class DataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

	private final FeriasRepository feriasRepository;

	public DataSeeder(FeriasRepository feriasRepository) {
		this.feriasRepository = feriasRepository;
	}

	@Override
	public void run(String... args) {
		List<Ferias> registros = List.of(
				new Ferias("1001", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30), 30),
				new Ferias("1002", LocalDate.of(2026, 3, 10), LocalDate.of(2026, 5, 31), 24),
				new Ferias("1003", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), 18),
				new Ferias("1004", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 6, 30), 30)
		);

		feriasRepository.saveAll(registros);

		// Loga quantos registros foram semeados no startup.
		log.info("Seed concluido: {} registros de ferias inseridos", registros.size());
	}
}
