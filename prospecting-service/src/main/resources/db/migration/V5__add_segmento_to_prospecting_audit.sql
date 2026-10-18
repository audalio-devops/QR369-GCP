-- ============================================================
-- Migration V5: Adicionar campo segmento em prospecting_audit
-- Projeto: prospecting-service | Banco: ragdb
-- ============================================================

-- 1. Adicionar coluna segmento com default CONTADORES
ALTER TABLE prospecting_audit
ADD COLUMN IF NOT EXISTS segmento VARCHAR(50) DEFAULT 'CONTADORES';

-- 2. Atualizar registros existentes para CONTADORES se nulos
UPDATE prospecting_audit
SET segmento = 'CONTADORES'
WHERE segmento IS NULL;

-- 3. Atualizar registros gerados nos testes do Luiggi para COMEX
UPDATE prospecting_audit
SET segmento = 'COMEX'
WHERE log LIKE '%COMEX%'
   OR log LIKE '%Comércio Exterior%'
   OR log LIKE '%Luiggi%';

-- 4. Criar índice para otimizar busca por segmento e data_evento
CREATE INDEX IF NOT EXISTS idx_audit_segmento_data_evento
ON prospecting_audit (segmento, data_evento DESC);

-- 5. Garantir coluna segmento em prospecting_data_source e índice
ALTER TABLE prospecting_data_source
ADD COLUMN IF NOT EXISTS segmento VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_datasource_segmento
ON prospecting_data_source (segmento);
