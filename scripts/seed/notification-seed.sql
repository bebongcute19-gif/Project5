-- Notification Service Seed Data
-- Database: notification_db

SET session_replication_role = replica;

-- Insert Notifications
INSERT INTO notifications (id, customer_id, event_type, title, message, status, is_read, related_entity_id, entity_type, created_at, updated_at)
VALUES
    -- Order notifications
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '11111111-1111-1111-1111-111111111111', 'ORDER_CREATED', 'Order Placed Successfully', 'Your order ORD-2025-001 has been placed successfully. Expected delivery in 5-7 business days.', 'READ', true, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'ORDER', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '11111111-1111-1111-1111-111111111111', 'ORDER_CONFIRMED', 'Order Confirmed', 'Your order ORD-2025-001 has been confirmed. Payment received.', 'READ', true, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'ORDER', NOW() - INTERVAL '6.5 days', NOW() - INTERVAL '6.5 days'),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '11111111-1111-1111-1111-111111111111', 'ORDER_SHIPPED', 'Order Shipped', 'Your order ORD-2025-001 has been shipped. Tracking number: TRK-123456', 'READ', true, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'ORDER', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', '11111111-1111-1111-1111-111111111111', 'ORDER_DELIVERED', 'Order Delivered', 'Your order ORD-2025-001 has been delivered. Thank you for shopping with us!', 'UNREAD', false, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'ORDER', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    
    -- Payment notifications
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', '33333333-3333-3333-3333-333333333333', 'PAYMENT_SUCCEEDED', 'Payment Confirmed', 'Your payment of 53,980,000 VND for order ORD-2025-002 has been successfully processed.', 'READ', true, 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'PAYMENT', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', '33333333-3333-3333-3333-333333333333', 'ORDER_CREATED', 'Order Placed', 'Your order ORD-2025-002 has been placed. Waiting for confirmation.', 'READ', true, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'ORDER', NOW() - INTERVAL '6.5 days', NOW() - INTERVAL '6.5 days'),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', '33333333-3333-3333-3333-333333333333', 'ORDER_PROCESSING', 'Order Processing', 'Your order ORD-2025-002 is now being processed and will be shipped soon.', 'UNREAD', false, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'ORDER', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    
    -- Order notifications for other customers
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', '55555555-5555-5555-5555-555555555555', 'ORDER_CREATED', 'Order Placed Successfully', 'Your order ORD-2025-003 has been placed. Payment is pending.', 'UNREAD', false, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'ORDER', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', '55555555-5555-5555-5555-555555555555', 'PAYMENT_PROCESSING', 'Payment Processing', 'Your payment for order ORD-2025-003 is being processed. Please wait.', 'UNREAD', false, 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'PAYMENT', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a0a', '77777777-7777-7777-7777-777777777777', 'ORDER_CREATED', 'Order Placed', 'Your order ORD-2025-004 has been received.', 'READ', true, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'ORDER', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a0b', '77777777-7777-7777-7777-777777777777', 'PAYMENT_SUCCEEDED', 'Payment Confirmed', 'Your payment for order ORD-2025-004 has been confirmed.', 'READ', true, 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'PAYMENT', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a0c', '99999999-9999-9999-9999-999999999999', 'ORDER_CREATED', 'Order Placed', 'Your order ORD-2025-005 has been confirmed and is being prepared.', 'READ', true, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'ORDER', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a0d', '99999999-9999-9999-9999-999999999999', 'ORDER_DELIVERED', 'Order Delivered', 'Your order ORD-2025-005 has arrived. We hope you enjoy your purchase!', 'READ', true, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'ORDER', NOW() - INTERVAL '2.5 days', NOW() - INTERVAL '2.5 days'),
    
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a0e', '11111111-1111-1111-1111-111111111111', 'ORDER_CANCELLED', 'Order Cancelled', 'Your order ORD-2025-006 has been cancelled. Refund will be processed within 3-5 business days.', 'UNREAD', false, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'ORDER', NOW() - INTERVAL '1 days', NOW() - INTERVAL '1 days'),
    
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a0f', '33333333-3333-3333-3333-333333333333', 'ORDER_PENDING', 'Order Pending', 'Your order ORD-2025-007 is awaiting payment confirmation.', 'UNREAD', false, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'ORDER', NOW() - INTERVAL '1 days', NOW() - INTERVAL '1 days'),
    
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', '55555555-5555-5555-5555-555555555555', 'ORDER_CONFIRMED', 'Order Confirmed', 'Your order ORD-2025-008 has been confirmed and will be shipped tomorrow.', 'UNREAD', false, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'ORDER', NOW(), NOW()),
    
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '77777777-7777-7777-7777-777777777777', 'ORDER_SHIPPED', 'Order Shipped', 'Your order ORD-2025-009 has been shipped. Tracking: TRK-789012', 'UNREAD', false, 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'ORDER', NOW(), NOW()),
    
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', '99999999-9999-9999-9999-999999999999', 'PAYMENT_FAILED', 'Payment Failed', 'Your payment for order ORD-2025-010 failed. Card declined. Please try another payment method.', 'UNREAD', false, 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 'PAYMENT', NOW(), NOW())
ON CONFLICT DO NOTHING;

SET session_replication_role = DEFAULT;

-- Verify data insertion
SELECT 'Notifications inserted: ' || COUNT(*) FROM notifications;
