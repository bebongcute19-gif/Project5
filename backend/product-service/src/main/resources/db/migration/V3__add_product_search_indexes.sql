-- Indexes for PostgreSQL product search optimization
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_product_variants_prices ON product_variants(product_id, active, regular_price, sale_price);
