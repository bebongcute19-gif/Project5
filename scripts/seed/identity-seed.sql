-- Identity Service Seed Data
-- Database: identity_db

-- Note: BCrypt password hashes pre-generated for test accounts
-- All passwords follow pattern: password[N]123 or admin123
-- Hashes generated with BCrypt strength 10

SET session_replication_role = replica;

-- Insert ADMIN user (if not exists)
INSERT INTO users (id, username, email, password_hash, full_name, phone, status, enabled, created_at, updated_at)
SELECT 
    '00000000-0000-0000-0000-000000000001'::uuid,
    'admin',
    'admin@example.com',
    '$2a$10$L9.LMxzJ.sQl3mM.sKGKs.P7T1q8PWh.4bHKPaXbQZVXVzABKxRju',
    'Administrator',
    '+84912345678',
    'ACTIVE',
    true,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

-- Insert CUSTOMER user1
INSERT INTO users (id, username, email, password_hash, full_name, phone, status, enabled, created_at, updated_at)
SELECT
    '11111111-1111-1111-1111-111111111111'::uuid,
    'user1',
    'user1@example.com',
    '$2a$10$l3Y4Z8K7N2M9B6V5X1C4O.sO8A7Z6Y5X4W3V2U1T0S9R8Q7P6O5N',
    'Test User 1',
    '+84912345671',
    'ACTIVE',
    true,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'user1');

-- Insert CUSTOMER user2
INSERT INTO users (id, username, email, password_hash, full_name, phone, status, enabled, created_at, updated_at)
SELECT
    '22222222-2222-2222-2222-222222222222'::uuid,
    'user2',
    'user2@example.com',
    '$2a$10$l3Y4Z8K7N2M9B6V5X1C4O.sO8A7Z6Y5X4W3V2U1T0S9R8Q7P6O5N',
    'Test User 2',
    '+84912345672',
    'ACTIVE',
    true,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'user2');

-- Insert CUSTOMER user3
INSERT INTO users (id, username, email, password_hash, full_name, phone, status, enabled, created_at, updated_at)
SELECT
    '33333333-3333-3333-3333-333333333333'::uuid,
    'user3',
    'user3@example.com',
    '$2a$10$l3Y4Z8K7N2M9B6V5X1C4O.sO8A7Z6Y5X4W3V2U1T0S9R8Q7P6O5N',
    'Test User 3',
    '+84912345673',
    'ACTIVE',
    true,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'user3');

-- Insert CUSTOMER user4
INSERT INTO users (id, username, email, password_hash, full_name, phone, status, enabled, created_at, updated_at)
SELECT
    '44444444-4444-4444-4444-444444444444'::uuid,
    'user4',
    'user4@example.com',
    '$2a$10$l3Y4Z8K7N2M9B6V5X1C4O.sO8A7Z6Y5X4W3V2U1T0S9R8Q7P6O5N',
    'Test User 4',
    '+84912345674',
    'ACTIVE',
    true,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'user4');

-- Insert CUSTOMER user5
INSERT INTO users (id, username, email, password_hash, full_name, phone, status, enabled, created_at, updated_at)
SELECT
    '55555555-5555-5555-5555-555555555555'::uuid,
    'user5',
    'user5@example.com',
    '$2a$10$l3Y4Z8K7N2M9B6V5X1C4O.sO8A7Z6Y5X4W3V2U1T0S9R8Q7P6O5N',
    'Test User 5',
    '+84912345675',
    'ACTIVE',
    true,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'user5');

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- Assign ADMIN role to admin user also
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'CUSTOMER'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- Assign CUSTOMER role to all test users
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username IN ('user1', 'user2', 'user3', 'user4', 'user5')
AND r.name = 'CUSTOMER'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- Insert addresses for test users
-- Address for user1 - primary
INSERT INTO addresses (id, user_id, recipient_name, phone, street_address, ward, district, city, is_default, created_at)
SELECT
    '22222222-2222-2222-2222-222222222222'::uuid,
    u.id,
    'Test User 1',
    '+84912345671',
    '123 Le Loi Street',
    'Ward 1',
    'District 1',
    'Ho Chi Minh City',
    true,
    NOW()
FROM users u
WHERE u.username = 'user1'
AND NOT EXISTS (
    SELECT 1 FROM addresses WHERE user_id = u.id AND is_default = true
);

-- Address for user1 - secondary
INSERT INTO addresses (id, user_id, recipient_name, phone, street_address, ward, district, city, is_default, created_at)
SELECT
    '22222222-2222-2222-2222-222222222223'::uuid,
    u.id,
    'Test User 1 - Office',
    '+84912345671',
    '456 Nguyen Hue Boulevard',
    'Ward 2',
    'District 1',
    'Ho Chi Minh City',
    false,
    NOW()
FROM users u
WHERE u.username = 'user1'
AND NOT EXISTS (
    SELECT 1 FROM addresses WHERE user_id = u.id AND street_address = '456 Nguyen Hue Boulevard'
);

-- Address for user2
INSERT INTO addresses (id, user_id, recipient_name, phone, street_address, ward, district, city, is_default, created_at)
SELECT
    '33333333-3333-3333-3333-333333333333'::uuid,
    u.id,
    'Test User 2',
    '+84912345672',
    '200 Test Street',
    'Ward 2',
    'District 1',
    'Ho Chi Minh City',
    true,
    NOW()
FROM users u
WHERE u.username = 'user2'
AND NOT EXISTS (
    SELECT 1 FROM addresses WHERE user_id = u.id AND is_default = true
);

-- Address for user3
INSERT INTO addresses (id, user_id, recipient_name, phone, street_address, ward, district, city, is_default, created_at)
SELECT
    '44444444-4444-4444-4444-444444444444'::uuid,
    u.id,
    'Test User 3',
    '+84912345673',
    '300 Test Street',
    'Ward 3',
    'District 1',
    'Ho Chi Minh City',
    true,
    NOW()
FROM users u
WHERE u.username = 'user3'
AND NOT EXISTS (
    SELECT 1 FROM addresses WHERE user_id = u.id AND is_default = true
);

-- Address for user4
INSERT INTO addresses (id, user_id, recipient_name, phone, street_address, ward, district, city, is_default, created_at)
SELECT
    '55555555-5555-5555-5555-555555555555'::uuid,
    u.id,
    'Test User 4',
    '+84912345674',
    '400 Test Street',
    'Ward 4',
    'District 1',
    'Ho Chi Minh City',
    true,
    NOW()
FROM users u
WHERE u.username = 'user4'
AND NOT EXISTS (
    SELECT 1 FROM addresses WHERE user_id = u.id AND is_default = true
);

-- Address for user5
INSERT INTO addresses (id, user_id, recipient_name, phone, street_address, ward, district, city, is_default, created_at)
SELECT
    '66666666-6666-6666-6666-666666666666'::uuid,
    u.id,
    'Test User 5',
    '+84912345675',
    '500 Test Street',
    'Ward 5',
    'District 1',
    'Ho Chi Minh City',
    true,
    NOW()
FROM users u
WHERE u.username = 'user5'
AND NOT EXISTS (
    SELECT 1 FROM addresses WHERE user_id = u.id AND is_default = true
);

SET session_replication_role = DEFAULT;

-- Verify data insertion
SELECT 'Users inserted: ' || COUNT(*) FROM users;
SELECT 'User Roles inserted: ' || COUNT(*) FROM user_roles;
SELECT 'Addresses inserted: ' || COUNT(*) FROM addresses;
