-- Product Events Outbox Table for Transactional Outbox Pattern
-- Ensures reliable synchronization between PostgreSQL and Elasticsearch

CREATE TABLE IF NOT EXISTS product_events_outbox (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_payload JSONB NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT chk_event_type CHECK (event_type IN ('PRODUCT_CREATED', 'PRODUCT_UPDATED', 'PRODUCT_STATUS_CHANGED', 'PRODUCT_DELETED', 'VARIANT_PRICE_CHANGED'))
);

CREATE INDEX IF NOT EXISTS idx_product_events_processed ON product_events_outbox(processed);
CREATE INDEX IF NOT EXISTS idx_product_events_product_id ON product_events_outbox(product_id);
CREATE INDEX IF NOT EXISTS idx_product_events_created_at ON product_events_outbox(created_at);
CREATE INDEX IF NOT EXISTS idx_product_events_retry_count ON product_events_outbox(retry_count);
CREATE INDEX IF NOT EXISTS idx_product_events_not_processed ON product_events_outbox(processed, retry_count) WHERE processed = false AND retry_count < max_retries;
