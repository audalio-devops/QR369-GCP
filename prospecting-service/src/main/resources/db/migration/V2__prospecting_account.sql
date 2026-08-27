-- ============================================================
-- Sprint 1: Prospecção de Contadores - Alterações no BD
-- Projeto: prospecting-service | Banco: ragdb
-- ============================================================

-- 1. Adicionar coluna 'status' na tabela existente
ALTER TABLE prospecting_data_source
ADD COLUMN IF NOT EXISTS status VARCHAR(100);

-- 2. Nova tabela: registros processados
CREATE TABLE IF NOT EXISTS prospecting_processed (
    id              BIGSERIAL       PRIMARY KEY,
    cnpj            VARCHAR(14)     NOT NULL,
    email           VARCHAR(255),
    razao_social    VARCHAR(255),
    telefone_valido VARCHAR(20),
    data_contato    TIMESTAMP,
    data_resposta   TIMESTAMP,
    status          VARCHAR(100),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- 3. Nova tabela: auditoria de envios
CREATE TABLE IF NOT EXISTS prospecting_audit (
    id          BIGSERIAL       PRIMARY KEY,
    data_envio  TIMESTAMP       NOT NULL DEFAULT NOW(),
    cnpj        VARCHAR(14),
    status      VARCHAR(10)     NOT NULL, -- 'Ok' ou 'Error'
    log         TEXT
);

-- Índices para consultas de monitoramento
CREATE INDEX IF NOT EXISTS idx_audit_data_envio ON prospecting_audit (data_envio DESC);
CREATE INDEX IF NOT EXISTS idx_processed_cnpj   ON prospecting_processed (cnpj);
CREATE INDEX IF NOT EXISTS idx_datasource_status ON prospecting_data_source (status);
