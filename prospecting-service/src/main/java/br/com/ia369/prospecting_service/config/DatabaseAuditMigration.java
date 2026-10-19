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

            // 4. Garantir coluna segmento em prospecting_data_source
            jdbcTemplate.execute(
                    "DO $$ " +
                            "BEGIN " +
                            "    IF NOT EXISTS (" +
                            "        SELECT 1 FROM information_schema.columns " +
                            "        WHERE table_name = 'prospecting_data_source' AND column_name = 'segmento'" +
                            "    ) THEN " +
                            "        ALTER TABLE prospecting_data_source ADD COLUMN segmento VARCHAR(50); " +
                            "        CREATE INDEX IF NOT EXISTS idx_datasource_segmento ON prospecting_data_source (segmento); " +
                            "    END IF; " +
                            "END $$;");

            // 5. Garantir coluna segmento em prospecting_processed
            jdbcTemplate.execute(
                    "DO $$ " +
                            "BEGIN " +
                            "    IF NOT EXISTS (" +
                            "        SELECT 1 FROM information_schema.columns " +
                            "        WHERE table_name = 'prospecting_processed' AND column_name = 'segmento'" +
                            "    ) THEN " +
                            "        ALTER TABLE prospecting_processed ADD COLUMN segmento VARCHAR(50); " +
                            "        CREATE INDEX IF NOT EXISTS idx_processed_segmento ON prospecting_processed (segmento); " +
                            "    END IF; " +
                            "END $$;");

            // 6. Atualizar retroativamente o segmento em prospecting_processed e prospecting_audit com base em prospecting_data_source
            jdbcTemplate.execute(
                    "UPDATE prospecting_processed pp " +
                            "SET segmento = pds.segmento " +
                            "FROM prospecting_data_source pds " +
                            "WHERE pp.cnpj = pds.cnpj " +
                            "  AND (pp.segmento IS NULL OR pp.segmento != pds.segmento) " +
                            "  AND pds.segmento IS NOT NULL;");

            jdbcTemplate.execute(
                    "UPDATE prospecting_audit pa " +
                            "SET segmento = pds.segmento " +
                            "FROM prospecting_data_source pds " +
                            "WHERE pa.cnpj = pds.cnpj " +
                            "  AND (pa.segmento IS NULL OR pa.segmento != pds.segmento) " +
                            "  AND pds.segmento IS NOT NULL;");

            jdbcTemplate.execute(
                    "UPDATE prospecting_audit " +
                            "SET segmento = 'COMEX' " +
                            "WHERE segmento IS NULL " +
                            "  AND (log LIKE '%COMEX%' OR log LIKE '%Comércio Exterior%' OR log LIKE '%Luiggi%');");

            jdbcTemplate.execute(
                    "UPDATE prospecting_audit " +
                            "SET segmento = 'CONTADORES' " +
                            "WHERE segmento IS NULL;");

            log.info("Ajuste de schema e sincronização de segmentos em prospecting concluídos com sucesso.");
        } catch (Exception ex) {
            log.warn("Aviso na verificação de schema do prospecting_audit: {}", ex.getMessage());
        }
    }
}
