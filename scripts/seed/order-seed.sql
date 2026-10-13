-- Order Service Seed Data
-- Database: order_db

SET session_replication_role = replica;

-- Insert Inventory (stock for products)
INSERT INTO inventory (id, sku_code, total_quantity, available_quantity, reserved_quantity, version, updated_at)
VALUES
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'SKU-IP15P-BLK-256', 50, 50, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'SKU-IP15P-BLK-512', 40, 40, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'SKU-IP15P-GOLD-256', 35, 35, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'SKU-SGS24-BLK', 60, 58, 2, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'SKU-SGS24-GRAY', 60, 60, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'SKU-MBP16-512', 30, 28, 2, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'SKU-MBP16-1TB', 25, 25, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'SKU-DXPS15-512', 40, 39, 1, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'SKU-DXPS15-1TB', 35, 35, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 'SKU-SONY-WH1-BLK', 100, 98, 2, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'SKU-SONY-WH1-SLV', 100, 100, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'SKU-NK-AM90-40', 150, 148, 2, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'SKU-NK-AM90-42', 0, 0, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'SKU-IPAD-AIR-256', 45, 44, 1, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'SKU-IPAD-AIR-512', 40, 40, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'SKU-SG-TV55', 20, 20, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a17', 'SKU-YOGA-PURPLE', 200, 200, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a18', 'SKU-YOGA-BLUE', 200, 200, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a19', 'SKU-TENT-4P', 80, 80, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a20', 'SKU-AIRPODS-PRO', 120, 120, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a21', 'SKU-TABS9-256', 50, 50, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'SKU-NK-RUN-40', 160, 160, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a23', 'SKU-SONY-FX30', 15, 15, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a24', 'SKU-HUB-SMART', 70, 70, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a25', 'SKU-MOUSE-ERG', 300, 300, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a26', 'SKU-KB-RGB', 250, 250, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a27', 'SKU-HUB-USB', 180, 180, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a28', 'SKU-SSD-1TB', 90, 90, 0, 0, NOW()),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a29', 'SKU-MON-27-4K', 55, 55, 0, 0, NOW())
ON CONFLICT (sku_code) DO NOTHING;

-- Insert Orders (using test user IDs from identity_db)
-- NOTE: customer_id and shipping_address_id should reference actual users from identity_db
INSERT INTO orders (id, order_number, customer_id, status, total_amount, tax_amount, shipping_fee, shipping_address_id, created_at, updated_at)
VALUES
    ('e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'ORD-2025-001', '11111111-1111-1111-1111-111111111111', 'PENDING', 29990000.00, 2999000.00, 50000.00, '22222222-2222-2222-2222-222222222222', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    ('e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'ORD-2025-002', '33333333-3333-3333-3333-333333333333', 'CONFIRMED', 53980000.00, 5398000.00, 50000.00, '44444444-4444-4444-4444-444444444444', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
    ('e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'ORD-2025-003', '55555555-5555-5555-5555-555555555555', 'PROCESSING', 8990000.00, 899000.00, 50000.00, '66666666-6666-6666-6666-666666666666', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    ('e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'ORD-2025-004', '77777777-7777-7777-7777-777777777777', 'SHIPPED', 3480000.00, 348000.00, 50000.00, '88888888-8888-8888-8888-888888888888', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'ORD-2025-005', '99999999-9999-9999-9999-999999999999', 'DELIVERED', 12980000.00, 1298000.00, 50000.00, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'ORD-2025-006', '11111111-1111-1111-1111-111111111111', 'CANCELLED', 5490000.00, 549000.00, 0.00, '22222222-2222-2222-2222-222222222222', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 days'),
    ('e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'ORD-2025-007', '33333333-3333-3333-3333-333333333333', 'PENDING', 45980000.00, 4598000.00, 100000.00, '44444444-4444-4444-4444-444444444444', NOW() - INTERVAL '1 days', NOW() - INTERVAL '1 days'),
    ('e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'ORD-2025-008', '55555555-5555-5555-5555-555555555555', 'CONFIRMED', 2480000.00, 248000.00, 50000.00, '66666666-6666-6666-6666-666666666666', NOW(), NOW()),
    ('e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'ORD-2025-009', '77777777-7777-7777-7777-777777777777', 'PROCESSING', 24980000.00, 2498000.00, 50000.00, '88888888-8888-8888-8888-888888888888', NOW(), NOW()),
    ('e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 'ORD-2025-010', '99999999-9999-9999-9999-999999999999', 'SHIPPED', 7990000.00, 799000.00, 50000.00, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), NOW())
ON CONFLICT (order_number) DO NOTHING;

-- Insert Order Items
INSERT INTO order_items (id, order_id, sku_code, product_name, price, quantity)
VALUES
    -- Order 1: 1x iPhone 15 Pro Black 256GB
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'SKU-IP15P-BLK-256', 'iPhone 15 Pro', 27990000.00, 1),
    
    -- Order 2: 1x MacBook Pro 16 512GB + 1x AirPods Pro
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'SKU-MBP16-512', 'MacBook Pro 16', 42990000.00, 1),
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'SKU-AIRPODS-PRO', 'AirPods Pro (2nd Gen)', 5290000.00, 2),
    
    -- Order 3: 1x Sony Headphones Black
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'SKU-SONY-WH1-BLK', 'Sony WH-1000XM5', 7990000.00, 1),
    
    -- Order 4: 1x Mechanical Keyboard + 1x Wireless Mouse
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'SKU-KB-RGB', 'Mechanical Keyboard', 1990000.00, 1),
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'SKU-MOUSE-ERG', 'Wireless Mouse', 390000.00, 2),
    
    -- Order 5: 1x 55" QLED TV
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'SKU-SG-TV55', 'Samsung 55" QLED TV', 11490000.00, 1),
    
    -- Order 6: 1x Yoga Mat + 1x Camping Tent (CANCELLED order)
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'SKU-YOGA-PURPLE', 'Yoga Mat Premium', 790000.00, 1),
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a0a', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'SKU-TENT-4P', 'Camping Tent 4-Person', 1690000.00, 3),
    
    -- Order 7: 1x iPad Air 256GB + 1x USB Hub
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a0b', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'SKU-IPAD-AIR-256', 'iPad Air 11', 17490000.00, 1),
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a0c', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'SKU-HUB-USB', 'USB-C Hub', 690000.00, 3),
    
    -- Order 8: 1x Nike Running Shoes Size 40
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a0d', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'SKU-NK-RUN-40', 'Nike Running Shoes', 1990000.00, 1),
    
    -- Order 9: 1x Dell XPS 15 512GB + 1x Portable SSD
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a0e', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'SKU-DXPS15-512', 'Dell XPS 15', 33990000.00, 1),
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a0f', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'SKU-SSD-1TB', 'Portable SSD 1TB', 3490000.00, 1),
    
    -- Order 10: 1x Monitor 27" 4K
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 'SKU-MON-27-4K', 'Monitor 27" 4K', 6990000.00, 1)
ON CONFLICT DO NOTHING;

-- Insert Stock Reservations (some reservations for pending orders)
INSERT INTO stock_reservations (id, order_id, sku_code, quantity, status, expires_at, created_at)
VALUES
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'SKU-IP15P-BLK-256', 1, 'ACTIVE', NOW() + INTERVAL '24 hours', NOW()),
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'SKU-IPAD-AIR-256', 1, 'ACTIVE', NOW() + INTERVAL '24 hours', NOW()),
    ('a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'SKU-HUB-USB', 3, 'ACTIVE', NOW() + INTERVAL '24 hours', NOW())
ON CONFLICT DO NOTHING;

SET session_replication_role = DEFAULT;

-- Verify data insertion
SELECT 'Inventory inserted: ' || COUNT(*) FROM inventory;
SELECT 'Orders inserted: ' || COUNT(*) FROM orders;
SELECT 'Order Items inserted: ' || COUNT(*) FROM order_items;
SELECT 'Stock Reservations inserted: ' || COUNT(*) FROM stock_reservations;
