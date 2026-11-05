-- ==============================================================================
-- Minha Despensa Backend — Schema Global Catalog V3 (PostgreSQL 15)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS global_products (
    id VARCHAR(36) PRIMARY KEY,
    ean VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    brand VARCHAR(80),
    category VARCHAR(100) NOT NULL DEFAULT 'Geral',
    measure_unit VARCHAR(30) NOT NULL DEFAULT 'UNIT',
    net_weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    source VARCHAR(50) NOT NULL,
    occurrence_count BIGINT NOT NULL DEFAULT 1,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

-- Chave de unicidade global por EAN
CREATE UNIQUE INDEX IF NOT EXISTS ux_global_products_ean ON global_products(ean);

-- Índices de consulta e busca textual
CREATE INDEX IF NOT EXISTS idx_global_products_normalized_name ON global_products(normalized_name);
CREATE INDEX IF NOT EXISTS idx_global_products_brand ON global_products(brand);
CREATE INDEX IF NOT EXISTS idx_global_products_updated_at ON global_products(updated_at);
