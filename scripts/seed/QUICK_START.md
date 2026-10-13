# Project5 Seed Data - Quick Start Guide

## ✅ Status: COMPLETE & VERIFIED
All seed data has been successfully created and verified in PostgreSQL.

## 🧑 Test Accounts

### Login to Identity Service (Port 8081)
```
Admin User:
  Username: admin
  Password: admin123
  
Test Users:
  Username: user1 (to user5)
  Password: password123
```

## 🗄️ Database Records

| Service | Scope | Count |
|---------|-------|-------|
| Identity | Users | 7 |
| Identity | Addresses | 6 |
| Identity | User Roles | 8 |
| Product | Products | 20 |
| Product | Categories | 6 |
| Product | Brands | 5 |
| Product | Variants | 29 |
| Order | Orders | 10 |
| Order | Order Items | 30 |
| Order | Inventory | 29 |
| Payment | Payments | 10 |
| Payment | Transactions | 15 |
| Notification | Notifications | 18 |

## 🚀 Quick Postman Testing Steps

1. **Start Services**
   ```bash
   docker-compose -f docker-compose-local.yml up
   ```

2. **Check Health**
   ```
   GET http://localhost:8081/actuator/health
   ```

3. **Get Auth Token**
   ```
   POST http://localhost:8081/api/v1/auth/login
   {
     "username": "admin",
     "password": "admin123"
   }
   ```

4. **Test Endpoints**
   - Products: `GET http://localhost:8082/api/v1/products`
   - Orders: `GET http://localhost:8083/api/v1/orders`
   - Payments: `GET http://localhost:8084/api/v1/payments`
   - Notifications: `GET http://localhost:8085/api/v1/notifications`

## 📁 Seed Files Location

All files are in: `D:\Project5\scripts\seed\`

- `identity-seed.sql` - Users & addresses
- `product-seed.sql` - Products & categories
- `order-seed.sql` - Orders & inventory
- `payment-seed.sql` - Payments & transactions
- `notification-seed.sql` - Notifications
- `execute-seed.ps1` - PowerShell execution script
- `execute-seed.bat` - Windows batch script
- `README.md` - Full documentation
- `FINAL_VERIFICATION.txt` - Detailed verification report

## 🔄 Re-run Seed Data

```bash
# PowerShell
D:\Project5\scripts\seed\execute-seed.ps1

# Or Windows batch
D:\Project5\scripts\seed\execute-seed.bat
```

All scripts are idempotent - safe to run multiple times without duplication.

## 📖 Full Documentation

See `README.md` for comprehensive setup and testing workflows.

---
**Ready for Postman Testing!** 🎉
