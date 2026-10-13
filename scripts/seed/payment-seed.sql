-- Payment Service Seed Data
-- Database: payment_db

SET session_replication_role = replica;

-- Insert Payments (linked with Orders from order_db)
INSERT INTO payments (id, transaction_id, order_id, customer_id, amount, status, payment_method, failure_reason, external_transaction_id, created_at, updated_at)
VALUES
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'TXN-2025-001', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '11111111-1111-1111-1111-111111111111', 29990000.00, 'SUCCEEDED', 'CREDIT_CARD', NULL, 'EXT-TXN-001', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'TXN-2025-002', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '33333333-3333-3333-3333-333333333333', 53980000.00, 'SUCCEEDED', 'DEBIT_CARD', NULL, 'EXT-TXN-002', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'TXN-2025-003', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '55555555-5555-5555-5555-555555555555', 8990000.00, 'PROCESSING', 'CREDIT_CARD', NULL, 'EXT-TXN-003', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'TXN-2025-004', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', '77777777-7777-7777-7777-777777777777', 3480000.00, 'SUCCEEDED', 'PAYPAL', NULL, 'EXT-TXN-004', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'TXN-2025-005', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', '99999999-9999-9999-9999-999999999999', 12980000.00, 'SUCCEEDED', 'CREDIT_CARD', NULL, 'EXT-TXN-005', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'TXN-2025-006', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', '11111111-1111-1111-1111-111111111111', 5490000.00, 'CANCELLED', 'CREDIT_CARD', NULL, NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 days'),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'TXN-2025-007', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', '33333333-3333-3333-3333-333333333333', 45980000.00, 'PENDING', 'DEBIT_CARD', NULL, NULL, NOW() - INTERVAL '1 days', NOW() - INTERVAL '1 days'),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'TXN-2025-008', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', '55555555-5555-5555-5555-555555555555', 2480000.00, 'PROCESSING', 'CREDIT_CARD', NULL, NULL, NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'TXN-2025-009', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', '77777777-7777-7777-7777-777777777777', 24980000.00, 'SUCCEEDED', 'PAYPAL', NULL, 'EXT-TXN-009', NOW(), NOW()),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 'TXN-2025-010', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', '99999999-9999-9999-9999-999999999999', 7990000.00, 'FAILED', 'CREDIT_CARD', 'Card declined', NULL, NOW(), NOW())
ON CONFLICT (transaction_id) DO NOTHING;

-- Insert Payment Transactions
INSERT INTO payment_transactions (id, payment_id, type, status, reference, error_message, created_at)
VALUES
    -- Payment 1 transactions
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'AUTHORIZATION', 'SUCCEEDED', 'AUTH-001', NULL, NOW() - INTERVAL '7 days'),
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'CHARGE', 'SUCCEEDED', 'CHG-001', NULL, NOW() - INTERVAL '7 days'),
    
    -- Payment 2 transactions
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'AUTHORIZATION', 'SUCCEEDED', 'AUTH-002', NULL, NOW() - INTERVAL '6 days'),
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'CHARGE', 'SUCCEEDED', 'CHG-002', NULL, NOW() - INTERVAL '6 days'),
    
    -- Payment 3 transactions (processing)
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'AUTHORIZATION', 'PENDING', 'AUTH-003', NULL, NOW() - INTERVAL '5 days'),
    
    -- Payment 4 transactions
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'AUTHORIZATION', 'SUCCEEDED', 'AUTH-004', NULL, NOW() - INTERVAL '4 days'),
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'CHARGE', 'SUCCEEDED', 'CHG-004', NULL, NOW() - INTERVAL '4 days'),
    
    -- Payment 5 transactions
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'AUTHORIZATION', 'SUCCEEDED', 'AUTH-005', NULL, NOW() - INTERVAL '3 days'),
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'CHARGE', 'SUCCEEDED', 'CHG-005', NULL, NOW() - INTERVAL '3 days'),
    
    -- Payment 6 transaction (cancelled)
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'AUTHORIZATION', 'FAILED', 'AUTH-006', 'Cancelled by user', NOW() - INTERVAL '2 days'),
    
    -- Payment 7 transaction (pending)
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'AUTHORIZATION', 'PENDING', 'AUTH-007', NULL, NOW() - INTERVAL '1 days'),
    
    -- Payment 8 transactions (processing)
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'AUTHORIZATION', 'PENDING', 'AUTH-008', NULL, NOW()),
    
    -- Payment 9 transactions
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'AUTHORIZATION', 'SUCCEEDED', 'AUTH-009', NULL, NOW()),
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'CHARGE', 'SUCCEEDED', 'CHG-009', NULL, NOW()),
    
    -- Payment 10 transaction (failed)
    ('b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 'AUTHORIZATION', 'FAILED', 'AUTH-010', 'Card declined by issuer', NOW())
ON CONFLICT DO NOTHING;

SET session_replication_role = DEFAULT;

-- Verify data insertion
SELECT 'Payments inserted: ' || COUNT(*) FROM payments;
SELECT 'Payment Transactions inserted: ' || COUNT(*) FROM payment_transactions;
