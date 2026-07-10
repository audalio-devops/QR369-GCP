package br.com.ia369.prospecting_service.config;

import br.com.ia369.prospecting_service.entity.Cnpj;
import br.com.ia369.prospecting_service.repository.CnpjRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class CnpjDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CnpjDataSeeder.class);

    private final CnpjRepository cnpjRepository;

    public CnpjDataSeeder(CnpjRepository cnpjRepository) {
        this.cnpjRepository = cnpjRepository;
    }

    @Override
    public void run(String... args) {
        List<Cnpj> registros = List.of(
                new Cnpj("60701190000104", "BANCO BRADESCO S.A.", "BRADESCO", "ATIVA",
                        LocalDate.of(1943, 3, 10), "11", "37350000", "contato@bradesco.com.br",
                        "Cidade de Deus", "s/n", "Prédio Prata", "Vila Yara", "Osasco", "SP", "06029-900"),
                new Cnpj("33000167000101", "PETROLEO BRASILEIRO S.A. PETROBRAS", "PETROBRAS", "ATIVA",
                        LocalDate.of(1953, 10, 23), "21", "22244400", "sac@petrobras.com.br",
                        "Avenida República do Chile", "65", "", "Centro", "Rio de Janeiro", "RJ", "20031-912"));

        cnpjRepository.saveAll(registros);
        log.info("Seed concluído: {} registros de CNPJ inseridos", registros.size());
    }
}
