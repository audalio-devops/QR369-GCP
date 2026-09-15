-- ============================================================
-- Migration V3: Ajustes na auditoria de prospecção
-- Projeto: prospecting-service | Banco: ragdb
-- ============================================================

-- 1. Renomear coluna data_envio para data_evento
ALTER TABLE prospecting_audit RENAME COLUMN data_envio TO data_evento;

-- 2. Expandir tamanho da coluna status de VARCHAR(10) para VARCHAR(50)
ALTER TABLE prospecting_audit ALTER COLUMN status TYPE VARCHAR(50);

-- 3. Atualizar índice na coluna data_evento
DROP INDEX IF EXISTS idx_audit_data_envio;
CREATE INDEX IF NOT EXISTS idx_audit_data_evento ON prospecting_audit (data_evento DESC);
