-- ============================================================
-- Migration V4: Adicionar campo prioridade em prospecting_data_source
-- Projeto: prospecting-service | Banco: ragdb
-- ============================================================

-- 1. Adicionar coluna 'prioridade' com default 1
ALTER TABLE prospecting_data_source
ADD COLUMN IF NOT EXISTS prioridade NUMERIC(1) DEFAULT 1;

-- 2. Atualizar registros existentes para garantir valor default 1
UPDATE prospecting_data_source
SET prioridade = 1
WHERE prioridade IS NULL;

-- 3. Criar índice para otimizar busca de leads pendentes ordenados por prioridade ascendente
CREATE INDEX IF NOT EXISTS idx_datasource_status_prioridade
ON prospecting_data_source (status, prioridade ASC);
