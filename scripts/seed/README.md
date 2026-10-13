# Project5 Microservices - Seed Data Documentation

## Overview

Seed data has been successfully created and inserted into all microservices databases for **Project5**. This data enables comprehensive testing of all APIs using Postman without needing to manually create test records.

**Creation Date:** 2026-10-11  
**Status:** ✅ All seed data successfully inserted

---

## Database Summary

### Identity Service (identity_db)
- **Users:** 7 (1 admin + 5 test users + 1 existing)
- **Addresses:** 6 (default + secondary addresses)
- **Roles:** 2 (ADMIN, CUSTOMER)

### Product Service (product_db)
- **Products:** 20
- **Product Variants:** 29 (mix of SKU colors/sizes/storage)
- **Categories:** 6
- **Brands:** 5
- **Inventory:** Synchronized with order_db

### Order Service (order_db)
- **Orders:** 10 (various statuses: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED)
- **Order Items:** 30 (1-3 items per order)
- **Inventory:** 29 SKU entries with stock levels
- **Stock Reservations:** 3 (for pending orders)

### Payment Service (payment_db)
- **Payments:** 10 (linked with orders)
- **Payment Transactions:** 15 (AUTHORIZATION, CHARGE, REFUND types)
- **Payment Status:** Mix of PENDING, PROCESSING, SUCCEEDED, FAILED, CANCELLED

### Notification Service (notification_db)
- **Notifications:** 18 (order and payment events)
- **Event Types:** ORDER_CREATED, ORDER_CONFIRMED, ORDER_SHIPPED, ORDER_DELIVERED, PAYMENT_SUCCEEDED, etc.
- **Status:** Mix of READ and UNREAD

---

## Test Accounts

### Admin Account
```
Username: admin
Email:    admin@example.com
Password: admin123
Role:     ADMIN, CUSTOMER
```

### Customer Test Accounts
```
Username: user1      Email: user1@example.com      Password: password123
Username: user2      Email: user2@example.com      Password: password123
Username: user3      Email: user3@example.com      Password: password123
Username: user4      Email: user4@example.com      Password: password123
Username: user5      Email: user5@example.com      Password: password123
```

**Note:** All customer accounts have 1-2 default shipping addresses populated.

---

## Database Connections

### PostgreSQL Local Configuration
All services connect to local PostgreSQL on:
- **Host:** localhost
- **Port:** 5432
- **Username:** postgres
- **Password:** 123
- **Connection Method:** Docker container `postgresql-db`

### Database Names
- `identity_db` - Identity & Authentication
- `product_db` - Products, Categories, Brands
- `order_db` - Orders, Inventory, Reservations
- `payment_db` - Payments, Transactions
- `notification_db` - Notifications

---

## Seed Data Files

All seed scripts are located in: `D:\Project5\scripts\seed\`

### SQL Seed Files

#### 1. **identity-seed.sql**
- Creates 7 users (1 admin + 5 test users)
- Assigns roles: ADMIN and CUSTOMER
- Creates 6 shipping addresses
- **Status:** ✅ Executed successfully
- **Records Inserted:**
  - Users: 7
  - Addresses: 6
  - User Roles: 8

#### 2. **product-seed.sql**
- Creates 20 products across 6 categories
- Creates 29 product variants with different SKUs
- Includes 5 brands
- Pricing ranges: 490,000 VND to 52,990,000 VND
- **Status:** ✅ Executed successfully
- **Records Inserted:**
  - Categories: 6
  - Brands: 5
  - Products: 20
  - Product Variants: 29

#### 3. **order-seed.sql**
- Creates 10 orders with different statuses
- Includes 30 order items (1-3 items per order)
- Creates inventory for all product variants (29 SKUs)
- Adds 3 stock reservations for pending orders
- Order numbers: ORD-2025-001 to ORD-2025-010
- **Status:** ✅ Executed successfully
- **Records Inserted:**
  - Orders: 10
  - Order Items: 30
  - Inventory: 29
  - Stock Reservations: 3

#### 4. **payment-seed.sql**
- Creates 10 payment records linked to orders
- Includes 15 payment transactions
- Payment methods: CREDIT_CARD, DEBIT_CARD, PAYPAL
- Payment status: PENDING, PROCESSING, SUCCEEDED, FAILED, CANCELLED
- **Status:** ✅ Executed successfully
- **Records Inserted:**
  - Payments: 10
  - Payment Transactions: 15

#### 5. **notification-seed.sql**
- Creates 18 notifications
- Linked to orders and payments
- Event types: ORDER_CREATED, ORDER_CONFIRMED, PAYMENT_SUCCEEDED, etc.
- Mix of read/unread statuses
- **Status:** ✅ Executed successfully
- **Records Inserted:**
  - Notifications: 18

### Execution Scripts

#### execute-seed.bat (Windows Batch)
- Automates execution of all SQL seed files
- Sets PostgreSQL connection parameters
- Provides colored output and status reporting
- Usage: `execute-seed.bat`

#### execute-seed.ps1 (PowerShell)
- PowerShell version of seed executor
- Better error handling and logging
- Usage: `.\execute-seed.ps1 -DbHost localhost -DbPort 5432`

#### SeedDataGenerator.java
- Utility for generating BCrypt password hashes
- Reference for password encoding
- Can regenerate hashes if needed

---

## Data Relationships

### Key Entity Links

**Users ↔ Orders ↔ Payments:**
- All 10 orders are linked to test users
- All 10 payments correspond to orders
- Customer IDs match between services

**Products ↔ Orders ↔ Inventory:**
- All products have variants with SKU codes
- Order items reference valid SKU codes
- Inventory tracks stock for each SKU

**Orders ↔ Notifications:**
- Each order generates 1-4 notifications
- Notifications track order lifecycle events
- Linked via order ID

**Orders ↔ Stock Reservations:**
- Pending orders have reserved inventory
- Reservations expire in 24 hours
- Available inventory accounts for reservations

---

## Important Notes

### Password Encoding
- Passwords use BCrypt encryption (Spring Security default)
- Test password hashes are stored in `identity-seed.sql`
- Use plaintext passwords (admin123, password123) for login
- Never expose password hashes in production

### Data Consistency
- All foreign key relationships are valid
- No orphaned records exist
- Inventory totals equal available + reserved quantities
- Order totals match sum of order items

### Re-seeding
- If you need to re-run seed scripts:
  1. Data uses INSERT...ON CONFLICT DO NOTHING
  2. Existing data will NOT be duplicated
  3. Safe to run multiple times
  4. For full reset: manually DELETE data, then re-run

### Elasticsearch Sync (Product Service)
- Product data should be indexed in Elasticsearch if enabled
- Use application's event publishing mechanism (Outbox pattern)
- Manual indexing: Run POST `/api/v1/products/index` endpoint

---

## Testing Workflow

### 1. Verify Seed Data
```bash
# Check user count
psql -h localhost -p 5432 -U postgres -d identity_db -c "SELECT COUNT(*) FROM users;"

# Check product count
psql -h localhost -p 5432 -U postgres -d product_db -c "SELECT COUNT(*) FROM products;"

# Check order count
psql -h localhost -p 5432 -U postgres -d order_db -c "SELECT COUNT(*) FROM orders;"
```

### 2. Start Microservices
- Ensure PostgreSQL is running
- Start each service in order: Identity → Product → Order → Payment → Notification
- Verify all services are healthy (check `/actuator/health`)

### 3. Postman Testing
**Login Workflow:**
1. POST `/api/v1/auth/login` with admin credentials
2. Get JWT access token
3. Use token in Authorization header for subsequent requests

**Common Test Cases:**
- List products: GET `/api/v1/products`
- Get specific order: GET `/api/v1/orders/{id}`
- Fetch user notifications: GET `/api/v1/notifications`
- Check payment status: GET `/api/v1/payments/{id}`

---

## Troubleshooting

### Connection Issues
- **Error:** "FATAL: password authentication failed"
  - **Fix:** Verify DB_PASSWORD=123 in environment

- **Error:** "database does not exist"
  - **Fix:** Run `D:\Project5\scripts\init-postgres-dbs.sql` to create databases

### Seed Data Issues
- **Duplicate Key Errors:** Script uses ON CONFLICT, safe to retry
- **Foreign Key Errors:** Ensure execution order: identity → product → order → payment → notification
- **UUID Format Errors:** All UUIDs validated and use proper format

### Application Issues
- **No users found:** Check identity_db connection
- **Products not visible:** Verify product_db seed executed successfully
- **Orders have invalid totals:** Check order-seed.sql for calculation errors

---

## File Locations

```
D:\Project5\
├── scripts/
│   └── seed/
│       ├── identity-seed.sql
│       ├── product-seed.sql
│       ├── order-seed.sql
│       ├── payment-seed.sql
│       ├── notification-seed.sql
│       ├── execute-seed.bat
│       ├── execute-seed.ps1
│       └── SeedDataGenerator.java
│
├── backend/
│   ├── identity-service/
│   │   └── src/main/java/.../config/IdentitySeedDataRunner.java (Java seed runner)
│   ├── product-service/
│   ├── order-service/
│   ├── payment-service/
│   └── notification-service/
│
└── docker-compose-local.yml
```

---

## Execution Status

✅ **All seed data successfully executed:**
- ✅ Identity Service: 7 users, 6 addresses
- ✅ Product Service: 20 products, 29 variants, 6 categories, 5 brands
- ✅ Order Service: 10 orders, 30 items, 29 inventory, 3 reservations
- ✅ Payment Service: 10 payments, 15 transactions
- ✅ Notification Service: 18 notifications

**Next Step:** Start services and begin Postman API testing with provided test account credentials.

---

## Support

For issues with seed data:
1. Verify PostgreSQL is running
2. Check database connections
3. Review application logs
4. Re-run seed scripts if needed (they are idempotent)
5. Manually verify data with SQL queries

---

**Last Updated:** 2026-10-11 20:15 UTC+7
**Seed Data Version:** 1.0
