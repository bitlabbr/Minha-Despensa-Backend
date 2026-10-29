-- ==============================================================================
-- Minha Despensa Backend — Schema V2: Módulo de Suporte e Coleta de Bugs
-- ==============================================================================

CREATE TABLE IF NOT EXISTS bug_reports (
    id VARCHAR(36) PRIMARY KEY,
    protocol VARCHAR(32) NOT NULL UNIQUE,
    category VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    app_version VARCHAR(64) NOT NULL,
    device_model VARCHAR(128),
    os_version VARCHAR(64),
    user_id VARCHAR(36),
    household_id VARCHAR(36),
    user_contact VARCHAR(255),
    has_logs BOOLEAN NOT NULL DEFAULT FALSE,
    log_file_path VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bug_reports_household ON bug_reports(household_id);
CREATE INDEX IF NOT EXISTS idx_bug_reports_status ON bug_reports(status);
CREATE INDEX IF NOT EXISTS idx_bug_reports_created_at ON bug_reports(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bug_reports_category ON bug_reports(category);
