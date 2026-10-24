-- ==============================================================================
-- Minha Despensa Backend — Schema Multi-Tenant V1 (PostgreSQL 15)
-- ==============================================================================

-- 1. Usuários e Autenticação
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    default_household_id VARCHAR(36),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    family_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at BIGINT NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_family_id ON refresh_tokens(family_id);

-- 2. Casas Compartilhadas (Households) e Membros
CREATE TABLE IF NOT EXISTS households (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    invite_code VARCHAR(32) NOT NULL UNIQUE,
    owner_id VARCHAR(36) NOT NULL REFERENCES users(id),
    sync_version BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS household_members (
    id VARCHAR(36) PRIMARY KEY,
    household_id VARCHAR(36) NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    joined_at BIGINT NOT NULL,
    CONSTRAINT uq_household_member UNIQUE (household_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_household_members_user_id ON household_members(user_id);

-- 3. Tabela de Aliases de Reconciliação Durável de IDs
CREATE TABLE IF NOT EXISTS entity_id_aliases (
    household_id VARCHAR(36) NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    alias_id VARCHAR(36) NOT NULL,
    canonical_id VARCHAR(36) NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (household_id, entity_type, alias_id)
);

-- 4. Categorias de Produtos
CREATE TABLE IF NOT EXISTS product_categories (
    household_id VARCHAR(36) NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at BIGINT NOT NULL,
    server_version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (household_id, id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_categories_household_name ON product_categories(household_id, normalized_name) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_categories_server_version ON product_categories(household_id, server_version);

-- 5. Catálogo Mestre de Produtos
CREATE TABLE IF NOT EXISTS catalog_products (
    household_id VARCHAR(36) NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    id VARCHAR(36) NOT NULL,
    ean VARCHAR(32),
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL DEFAULT 'Geral',
    brand VARCHAR(80),
    measure_unit VARCHAR(30) NOT NULL DEFAULT 'UNIT',
    net_weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    thumbnail_url VARCHAR(1024),
    image_hash VARCHAR(64),
    manually_added BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(2000),
    updated_at BIGINT NOT NULL,
    server_version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (household_id, id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_products_household_ean ON catalog_products(household_id, ean) WHERE ean IS NOT NULL AND is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS ux_products_household_name_unit ON catalog_products(household_id, normalized_name, measure_unit) WHERE ean IS NULL AND is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_products_server_version ON catalog_products(household_id, server_version);

-- 6. Mídia de Produtos (Fotos / Blobs WebP)
CREATE TABLE IF NOT EXISTS product_media (
    household_id VARCHAR(36) NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    product_id VARCHAR(36) NOT NULL,
    content_type VARCHAR(100) NOT NULL DEFAULT 'image/webp',
    sha256_hash VARCHAR(64) NOT NULL,
    image_bytes BYTEA NOT NULL,
    updated_at BIGINT NOT NULL,
    server_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (household_id, product_id)
);

-- 7. Listas de Compras
CREATE TABLE IF NOT EXISTS shopping_lists (
    household_id VARCHAR(36) NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    list_type VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    list_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    budget_in_cents BIGINT,
    updated_at BIGINT NOT NULL,
    server_version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (household_id, id)
);

CREATE INDEX IF NOT EXISTS idx_shopping_lists_server_version ON shopping_lists(household_id, server_version);

-- 8. Itens das Listas de Compras
CREATE TABLE IF NOT EXISTS shopping_items (
    household_id VARCHAR(36) NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    id VARCHAR(36) NOT NULL,
    list_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36),
    raw_text VARCHAR(255),
    quantity DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    price_at_time BIGINT,
    is_checked BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at BIGINT NOT NULL,
    server_version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (household_id, id)
);

CREATE INDEX IF NOT EXISTS idx_shopping_items_list ON shopping_items(household_id, list_id);
CREATE INDEX IF NOT EXISTS idx_shopping_items_server_version ON shopping_items(household_id, server_version);

-- 9. Itens da Despensa (Estoque / Lotes)
CREATE TABLE IF NOT EXISTS pantry_items (
    household_id VARCHAR(36) NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    expiration_date BIGINT,
    batch_number VARCHAR(100),
    unit_price_in_cents BIGINT,
    updated_at BIGINT NOT NULL,
    server_version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (household_id, id)
);

CREATE INDEX IF NOT EXISTS idx_pantry_items_product ON pantry_items(household_id, product_id);
CREATE INDEX IF NOT EXISTS idx_pantry_items_server_version ON pantry_items(household_id, server_version);

-- 10. Histórico de Preços
CREATE TABLE IF NOT EXISTS price_entries (
    household_id VARCHAR(36) NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    price_in_cents BIGINT NOT NULL,
    store_name VARCHAR(255) NOT NULL DEFAULT 'Supermercado',
    updated_at BIGINT NOT NULL,
    server_version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (household_id, id)
);

CREATE INDEX IF NOT EXISTS idx_price_entries_product ON price_entries(household_id, product_id);
CREATE INDEX IF NOT EXISTS idx_price_entries_server_version ON price_entries(household_id, server_version);

-- 11. Movimentações de Despensa (Livro-Razão)
CREATE TABLE IF NOT EXISTS pantry_movements (
    household_id VARCHAR(36) NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    id VARCHAR(36) NOT NULL,
    pantry_item_id VARCHAR(36),
    product_id VARCHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    unit_price_in_cents BIGINT,
    total_price_in_cents BIGINT,
    store_or_reason VARCHAR(255),
    updated_at BIGINT NOT NULL,
    server_version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (household_id, id)
);

CREATE INDEX IF NOT EXISTS idx_pantry_movements_product ON pantry_movements(household_id, product_id);
CREATE INDEX IF NOT EXISTS idx_pantry_movements_server_version ON pantry_movements(household_id, server_version);

-- 12. Orçamentos Mensais
CREATE TABLE IF NOT EXISTS monthly_budgets (
    household_id VARCHAR(36) NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    id VARCHAR(36) NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    budget_in_cents BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    server_version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (household_id, id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_budgets_household_year_month ON monthly_budgets(household_id, year, month);
CREATE INDEX IF NOT EXISTS idx_monthly_budgets_server_version ON monthly_budgets(household_id, server_version);
