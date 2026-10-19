-- ============================================================
-- Migration V6: Adicionar campo segmento em prospecting_processed
-- e corrigir segmento em prospecting_audit
-- Projeto: prospecting-service | Banco: ragdb
-- ============================================================

-- 1. Garantir coluna segmento em prospecting_processed
ALTER TABLE prospecting_processed
ADD COLUMN IF NOT EXISTS segmento VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_processed_segmento
ON prospecting_processed (segmento);

-- 2. Atualizar prospecting_processed com base no segmento de prospecting_data_source
UPDATE prospecting_processed pp
SET segmento = pds.segmento
FROM prospecting_data_source pds
WHERE pp.cnpj = pds.cnpj
  AND (pp.segmento IS NULL OR pp.segmento != pds.segmento)
  AND pds.segmento IS NOT NULL;

-- 3. Atualizar prospecting_audit com base no segmento de prospecting_data_source
UPDATE prospecting_audit pa
SET segmento = pds.segmento
FROM prospecting_data_source pds
WHERE pa.cnpj = pds.cnpj
  AND (pa.segmento IS NULL OR pa.segmento != pds.segmento)
  AND pds.segmento IS NOT NULL;

-- 4. Atualizar logs de auditoria do Luiggi / COMEX que estejam nulos
UPDATE prospecting_audit
SET segmento = 'COMEX'
WHERE segmento IS NULL
  AND (log LIKE '%COMEX%' OR log LIKE '%Comércio Exterior%' OR log LIKE '%Luiggi%');

-- 5. Atualizar quaisquer auditorias restantes sem segmento para CONTADORES
UPDATE prospecting_audit
SET segmento = 'CONTADORES'
WHERE segmento IS NULL;
