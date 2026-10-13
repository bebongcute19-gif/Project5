-- Product Service Seed Data
-- Database: product_db

-- Disable foreign key constraints temporarily
SET session_replication_role = replica;

-- Clear existing data (if needed for re-seeding)
-- DELETE FROM product_variants;
-- DELETE FROM products;
-- DELETE FROM brands;
-- DELETE FROM categories;

-- Insert Categories
INSERT INTO categories (id, name, slug, description, parent_id, active, created_at, updated_at)
VALUES
    ('f47ac10b-58cc-4372-a567-0e02b2c3d479', 'Electronics', 'electronics', 'Electronic devices and gadgets', NULL, true, NOW(), NOW()),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d480', 'Clothing', 'clothing', 'Apparel and fashion items', NULL, true, NOW(), NOW()),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d481', 'Home & Garden', 'home-garden', 'Home and garden products', NULL, true, NOW(), NOW()),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d482', 'Sports', 'sports', 'Sports equipment and accessories', NULL, true, NOW(), NOW()),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d483', 'Smartphones', 'smartphones', 'Mobile phones and accessories', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', true, NOW(), NOW()),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d484', 'Laptops', 'laptops', 'Computers and laptops', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', true, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- Insert Brands
INSERT INTO brands (id, name, slug, description, logo_url, active, created_at, updated_at)
VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Apple', 'apple', 'Premium technology brand', 'https://example.com/apple-logo.png', true, NOW(), NOW()),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'Samsung', 'samsung', 'Electronics manufacturer', 'https://example.com/samsung-logo.png', true, NOW(), NOW()),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'Nike', 'nike', 'Sports apparel company', 'https://example.com/nike-logo.png', true, NOW(), NOW()),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'Sony', 'sony', 'Consumer electronics', 'https://example.com/sony-logo.png', true, NOW(), NOW()),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'Dell', 'dell', 'Computer manufacturer', 'https://example.com/dell-logo.png', true, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- Insert Products
INSERT INTO products (id, name, slug, description, category_id, brand_id, status, images, created_at, updated_at)
VALUES
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'iPhone 15 Pro', 'iphone-15-pro', 'Latest Apple flagship smartphone with advanced camera', 'f47ac10b-58cc-4372-a567-0e02b2c3d483', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'ACTIVE', ARRAY['https://example.com/iphone15-1.jpg', 'https://example.com/iphone15-2.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'Samsung Galaxy S24', 'samsung-galaxy-s24', 'Samsung flagship with superior display', 'f47ac10b-58cc-4372-a567-0e02b2c3d483', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'ACTIVE', ARRAY['https://example.com/s24-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'MacBook Pro 16', 'macbook-pro-16', 'Professional laptop with M3 Max chip', 'f47ac10b-58cc-4372-a567-0e02b2c3d484', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'ACTIVE', ARRAY['https://example.com/mbp-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'Dell XPS 15', 'dell-xps-15', 'High-performance laptop with OLED display', 'f47ac10b-58cc-4372-a567-0e02b2c3d484', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'ACTIVE', ARRAY['https://example.com/xps-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'Sony WH-1000XM5', 'sony-wh1000xm5', 'Premium noise-cancelling headphones', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'ACTIVE', ARRAY['https://example.com/wh1000xm5-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'Nike Air Max 90', 'nike-air-max-90', 'Classic comfort sneaker', 'f47ac10b-58cc-4372-a567-0e02b2c3d480', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'ACTIVE', ARRAY['https://example.com/airmax-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'iPad Air 11', 'ipad-air-11', 'Versatile tablet with M2 chip', 'f47ac10b-58cc-4372-a567-0e02b2c3d483', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'ACTIVE', ARRAY['https://example.com/ipadair-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'Samsung 55" QLED TV', 'samsung-55-qled', '55-inch QLED smart television', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'ACTIVE', ARRAY['https://example.com/tv-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'Yoga Mat Premium', 'yoga-mat-premium', 'High-quality yoga mat for fitness', 'f47ac10b-58cc-4372-a567-0e02b2c3d482', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'ACTIVE', ARRAY['https://example.com/yoga-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 'Camping Tent 4-Person', 'camping-tent-4person', 'Waterproof tent for outdoor camping', 'f47ac10b-58cc-4372-a567-0e02b2c3d481', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'ACTIVE', ARRAY['https://example.com/tent-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'AirPods Pro (2nd Gen)', 'airpods-pro-2', 'Wireless earbuds with ANC', 'f47ac10b-58cc-4372-a567-0e02b2c3d483', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'ACTIVE', ARRAY['https://example.com/airpods-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'Samsung Tab S9 Ultra', 'samsung-tab-s9-ultra', '14.6-inch tablet with S Pen', 'f47ac10b-58cc-4372-a567-0e02b2c3d483', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'ACTIVE', ARRAY['https://example.com/tabs9-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'Nike Running Shoes', 'nike-running-shoes', 'Lightweight running shoes', 'f47ac10b-58cc-4372-a567-0e02b2c3d480', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'ACTIVE', ARRAY['https://example.com/runshoes-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'Sony FX30 Camera', 'sony-fx30-camera', 'Professional mirrorless camera', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'ACTIVE', ARRAY['https://example.com/fx30-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'Smart Home Hub', 'smart-home-hub', 'Smart home control center', 'f47ac10b-58cc-4372-a567-0e02b2c3d481', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'ACTIVE', ARRAY['https://example.com/hub-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'Wireless Mouse', 'wireless-mouse', 'Ergonomic wireless mouse', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'ACTIVE', ARRAY['https://example.com/mouse-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a17', 'Mechanical Keyboard', 'mechanical-keyboard', 'RGB mechanical gaming keyboard', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'ACTIVE', ARRAY['https://example.com/keyboard-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a18', 'USB-C Hub', 'usb-c-hub', 'Multi-port USB-C hub', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'ACTIVE', ARRAY['https://example.com/hub-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a19', 'Portable SSD 1TB', 'portable-ssd-1tb', 'Fast portable SSD storage', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'ACTIVE', ARRAY['https://example.com/ssd-1.jpg'], NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a20', 'Monitor 27" 4K', 'monitor-27-4k', '27-inch 4K display monitor', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'ACTIVE', ARRAY['https://example.com/monitor-1.jpg'], NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- Insert Product Variants
INSERT INTO product_variants (id, product_id, sku_code, variant_name, regular_price, sale_price, currency, attributes, active, created_at, updated_at)
VALUES
    -- iPhone 15 Pro variants
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'SKU-IP15P-BLK-256', 'Black 256GB', 29990000.00, 27990000.00, 'VND', '{"color": "Black", "storage": "256GB"}', true, NOW(), NOW()),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'SKU-IP15P-BLK-512', 'Black 512GB', 33990000.00, 31990000.00, 'VND', '{"color": "Black", "storage": "512GB"}', true, NOW(), NOW()),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'SKU-IP15P-GOLD-256', 'Gold 256GB', 29990000.00, 27990000.00, 'VND', '{"color": "Gold", "storage": "256GB"}', true, NOW(), NOW()),
    
    -- Samsung Galaxy S24
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'SKU-SGS24-BLK', 'Onyx Black', 22990000.00, 20990000.00, 'VND', '{"color": "Onyx Black"}', true, NOW(), NOW()),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'SKU-SGS24-GRAY', 'Amber Gray', 22990000.00, 20990000.00, 'VND', '{"color": "Amber Gray"}', true, NOW(), NOW()),
    
    -- MacBook Pro 16
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'SKU-MBP16-512', '512GB SSD', 44990000.00, 42990000.00, 'VND', '{"storage": "512GB", "ram": "18GB"}', true, NOW(), NOW()),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'SKU-MBP16-1TB', '1TB SSD', 52990000.00, 49990000.00, 'VND', '{"storage": "1TB", "ram": "18GB"}', true, NOW(), NOW()),
    
    -- Dell XPS 15
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'SKU-DXPS15-512', '512GB SSD', 35990000.00, 33990000.00, 'VND', '{"storage": "512GB", "ram": "16GB"}', true, NOW(), NOW()),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'SKU-DXPS15-1TB', '1TB SSD', 41990000.00, 39990000.00, 'VND', '{"storage": "1TB", "ram": "16GB"}', true, NOW(), NOW()),
    
    -- Sony Headphones
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'SKU-SONY-WH1-BLK', 'Black', 8990000.00, 7990000.00, 'VND', '{"color": "Black"}', true, NOW(), NOW()),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'SKU-SONY-WH1-SLV', 'Silver', 8990000.00, 7990000.00, 'VND', '{"color": "Silver"}', true, NOW(), NOW()),
    
    -- Nike Shoes
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'SKU-NK-AM90-40', 'Size 40', 2990000.00, 2490000.00, 'VND', '{"size": "40"}', true, NOW(), NOW()),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'SKU-NK-AM90-42', 'Size 42', 2990000.00, 2490000.00, 'VND', '{"size": "42"}', true, NOW(), NOW()),
    
    -- iPad Air
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'SKU-IPAD-AIR-256', '256GB WiFi', 18990000.00, 17490000.00, 'VND', '{"storage": "256GB", "connectivity": "WiFi"}', true, NOW(), NOW()),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'SKU-IPAD-AIR-512', '512GB WiFi', 24990000.00, 22990000.00, 'VND', '{"storage": "512GB", "connectivity": "WiFi"}', true, NOW(), NOW()),
    
    -- Samsung TV
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'SKU-SG-TV55', '55-inch QLED', 12990000.00, 11490000.00, 'VND', '{"size": "55", "type": "QLED"}', true, NOW(), NOW()),
    
    -- Yoga Mat
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a17', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'SKU-YOGA-PURPLE', 'Purple', 990000.00, 790000.00, 'VND', '{"color": "Purple"}', true, NOW(), NOW()),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a18', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'SKU-YOGA-BLUE', 'Blue', 990000.00, 790000.00, 'VND', '{"color": "Blue"}', true, NOW(), NOW()),
    
    -- Camping Tent
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a19', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 'SKU-TENT-4P', '4-Person Tent', 1990000.00, 1690000.00, 'VND', '{"capacity": "4-person"}', true, NOW(), NOW()),
    
    -- AirPods Pro
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a20', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'SKU-AIRPODS-PRO', 'Standard', 5990000.00, 5290000.00, 'VND', '{"model": "2nd Gen"}', true, NOW(), NOW()),
    
    -- Samsung Tab
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a21', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'SKU-TABS9-256', '256GB', 19990000.00, 18290000.00, 'VND', '{"storage": "256GB"}', true, NOW(), NOW()),
    
    -- Nike Running
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'SKU-NK-RUN-40', 'Size 40', 2490000.00, 1990000.00, 'VND', '{"size": "40"}', true, NOW(), NOW()),
    
    -- Sony Camera
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a23', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'SKU-SONY-FX30', 'Standard', 32990000.00, 29990000.00, 'VND', '{"model": "FX30"}', true, NOW(), NOW()),
    
    -- Smart Home Hub
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a24', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'SKU-HUB-SMART', 'Home Hub', 3990000.00, 3490000.00, 'VND', '{"type": "Smart Hub"}', true, NOW(), NOW()),
    
    -- Wireless Mouse
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a25', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'SKU-MOUSE-ERG', 'Ergonomic', 490000.00, 390000.00, 'VND', '{"type": "Ergonomic"}', true, NOW(), NOW()),
    
    -- Mechanical Keyboard
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a26', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a17', 'SKU-KB-RGB', 'RGB Mechanical', 2490000.00, 1990000.00, 'VND', '{"type": "RGB"}', true, NOW(), NOW()),
    
    -- USB-C Hub
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a27', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a18', 'SKU-HUB-USB', '7-in-1', 890000.00, 690000.00, 'VND', '{"ports": "7"}', true, NOW(), NOW()),
    
    -- Portable SSD
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a28', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a19', 'SKU-SSD-1TB', '1TB', 3990000.00, 3490000.00, 'VND', '{"capacity": "1TB"}', true, NOW(), NOW()),
    
    -- Monitor
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a29', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a20', 'SKU-MON-27-4K', '27 inch 4K', 7990000.00, 6990000.00, 'VND', '{"size": "27", "resolution": "4K"}', true, NOW(), NOW())
ON CONFLICT (sku_code) DO NOTHING;

-- Re-enable foreign key constraints
SET session_replication_role = DEFAULT;

-- Verify data insertion
SELECT 'Categories inserted: ' || COUNT(*) FROM categories;
SELECT 'Brands inserted: ' || COUNT(*) FROM brands;
SELECT 'Products inserted: ' || COUNT(*) FROM products;
SELECT 'Product Variants inserted: ' || COUNT(*) FROM product_variants;
