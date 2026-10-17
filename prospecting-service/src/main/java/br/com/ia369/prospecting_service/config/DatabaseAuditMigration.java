package br.com.ia369.prospecting_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DatabaseAuditMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAuditMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseAuditMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Executando verificação de schema para a tabela prospecting_audit...");

            // 1. Remover restrição NOT NULL da coluna legada data_envio se ela existir
            jdbcTemplate.execute(
                    "DO $$ " +
                            "BEGIN " +
                            "    IF EXISTS (" +
                            "        SELECT 1 FROM information_schema.columns " +
                            "        WHERE table_name = 'prospecting_audit' AND column_name = 'data_envio'" +
                            "    ) THEN " +
                            "        ALTER TABLE prospecting_audit ALTER COLUMN data_envio DROP NOT NULL; " +
                            "    END IF; " +
                            "END $$;");

            // 2. Garantir que a coluna status tenha tamanho 50
            jdbcTemplate.execute(
                    "DO $$ " +
                            "BEGIN " +
                            "    IF EXISTS (" +
                            "        SELECT 1 FROM information_schema.columns " +
                            "        WHERE table_name = 'prospecting_audit' AND column_name = 'status'" +
                            "    ) THEN " +
                            "        ALTER TABLE prospecting_audit ALTER COLUMN status TYPE VARCHAR(50); " +
                            "    END IF; " +
                            "END $$;");

            // 3. Garantir coluna segmento com default CONTADORES e índices
            jdbcTemplate.execute(
                    "DO $$ " +
                            "BEGIN " +
                            "    IF NOT EXISTS (" +
                            "        SELECT 1 FROM information_schema.columns " +
                            "        WHERE table_name = 'prospecting_audit' AND column_name = 'segmento'" +
                            "    ) THEN " +
                            "        ALTER TABLE prospecting_audit ADD COLUMN segmento VARCHAR(50) DEFAULT 'CONTADORES'; " +
                            "        UPDATE prospecting_audit SET segmento = 'CONTADORES' WHERE segmento IS NULL; " +
                            "        UPDATE prospecting_audit SET segmento = 'COMEX' WHERE log LIKE '%COMEX%' OR log LIKE '%Comércio Exterior%' OR log LIKE '%Luiggi%'; " +
                            "        CREATE INDEX IF NOT EXISTS idx_audit_segmento_data_evento ON prospecting_audit (segmento, data_evento DESC); " +
                            "    END IF; " +
                            "END $$;");

            log.info("Ajuste de schema em prospecting_audit concluído com sucesso.");
        } catch (Exception ex) {
            log.warn("Aviso na verificação de schema do prospecting_audit: {}", ex.getMessage());
        }
    }
}
