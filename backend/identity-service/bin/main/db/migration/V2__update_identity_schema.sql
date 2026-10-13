-- Add username column
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(100) UNIQUE;

-- Add enabled column
ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- Update refresh_tokens: add token_hash and revoked_at
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS token_hash VARCHAR(512);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP WITH TIME ZONE;

-- Seed default roles
INSERT INTO roles (name, description) VALUES ('ADMIN', 'System Administrator') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name, description) VALUES ('CUSTOMER', 'Customer') ON CONFLICT (name) DO NOTHING;

-- Create index on username
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
