# Project5 - Local Development Guide

## 🚀 Quick Start

### Prerequisites
- Windows 11 with PowerShell
- Docker Desktop running
- IntelliJ IDEA with JDK 17
- Gradle 8.5+
- Git

### Step 1: Start Infrastructure Services
```bash
cd d:\Project5
docker-compose -f docker-compose-local.yml up -d
```

Verify all services are healthy:
```bash
docker-compose -f docker-compose-local.yml ps
```

Expected output: All services showing "Up ... (healthy)"

### Step 2: Start Spring Boot Services from IntelliJ

Open IntelliJ IDEA and configure 6 Spring Boot run configurations:

#### For each service, set up:

**1. Identity Service**
- Main class: `com.omnichannel.identity.IdentityServiceApplication`
- Program args: `--spring.profiles.active=local`
- Port: 8081

**2. Product Service**
- Main class: `com.omnichannel.product.ProductServiceApplication`
- Program args: `--spring.profiles.active=local`
- Port: 8082
- Environment: `GRPC_PORT=9082`

**3. Order Service**
- Main class: `com.omnichannel.order.OrderServiceApplication`
- Program args: `--spring.profiles.active=local`
- Port: 8083
- Environment: `GRPC_PRODUCT_HOST=localhost;GRPC_PRODUCT_PORT=9082`

**4. Payment Service**
- Main class: `com.omnichannel.payment.PaymentServiceApplication`
- Program args: `--spring.profiles.active=local`
- Port: 8084

**5. Notification Service**
- Main class: `com.omnichannel.notification.NotificationServiceApplication`
- Program args: `--spring.profiles.active=local`
- Port: 8085

**6. API Gateway**
- Main class: `com.omnichannel.gateway.ApiGatewayApplication`
- Program args: `--spring.profiles.active=local`
- Port: 8080

See `INTELLIJ_SETUP.md` for detailed configuration instructions.

### Step 3: Verify All Services

Check all health endpoints:
```powershell
powershell -ExecutionPolicy Bypass -File test-services.ps1
```

Or manually:
```bash
curl http://localhost:8081/actuator/health  # Identity
curl http://localhost:8082/actuator/health  # Product
curl http://localhost:8083/actuator/health  # Order
curl http://localhost:8084/actuator/health  # Payment
curl http://localhost:8085/actuator/health  # Notification
curl http://localhost:8080/actuator/health  # API Gateway
```

## 📝 Common Tasks

### Test JWT Authentication

```bash
# Register new user
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d @test-register.json

# Login and get JWT token
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d @test-login.json

# Response will include accessToken
```

### Use JWT Token

```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# Call Product API with JWT
curl http://localhost:8082/api/v1/products \
  -H "Authorization: Bearer $TOKEN"

# Call via API Gateway (routes to Product Service)
curl http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN"
```

### Check RabbitMQ

Management UI: http://localhost:15672/
- Username: guest
- Password: guest

### Check Elasticsearch

Health check:
```bash
curl http://localhost:9200/_cat/health
```

Index status:
```bash
curl http://localhost:9200/_cat/indices
```

### Access Docker Container Logs

```bash
# PostgreSQL
docker-compose -f docker-compose-local.yml logs postgresql-db

# Redis
docker-compose -f docker-compose-local.yml logs redis

# RabbitMQ
docker-compose -f docker-compose-local.yml logs rabbitmq

# Elasticsearch
docker-compose -f docker-compose-local.yml logs elasticsearch
```

### Access Service Logs in IntelliJ

- Look in the IntelliJ Console panel for each running service
- Search for "Started" to confirm startup completion
- Look for "ERROR" to identify issues

### Rebuild a Single Service

```bash
cd d:\Project5
./gradlew :backend:product-service:clean :backend:product-service:build
```

### Rebuild All Services

```bash
cd d:\Project5
./gradlew clean build
```

## 🔧 Environment Variables

These variables are used by all services (set in run configurations):

```
# Database
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=123

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672

# Elasticsearch
ELASTICSEARCH_URIS=http://localhost:9200

# JWT Secret (same for all services)
JWT_SECRET=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==

# gRPC Configuration
GRPC_PRODUCT_HOST=localhost
GRPC_PRODUCT_PORT=9082

# Service Routing (for API Gateway)
IDENTITY_SERVICE_HOST=localhost
IDENTITY_SERVICE_PORT=8081
PRODUCT_SERVICE_HOST=localhost
PRODUCT_SERVICE_PORT=8082
ORDER_SERVICE_HOST=localhost
ORDER_SERVICE_PORT=8083
PAYMENT_SERVICE_HOST=localhost
PAYMENT_SERVICE_PORT=8084
NOTIFICATION_SERVICE_HOST=localhost
NOTIFICATION_SERVICE_PORT=8085
```

## 🐛 Troubleshooting

### Service fails to start: "Port already in use"

```bash
# Find process using port (e.g., 8081)
netstat -ano | findstr :8081

# Kill process
taskkill /PID <PID> /F
```

### Database migration fails

```bash
# Check PostgreSQL is running
docker-compose -f docker-compose-local.yml logs postgresql-db

# Check database was created
docker exec postgresql-db psql -U postgres -l
```

### Elasticsearch connection fails

```bash
# Check Elasticsearch health
curl http://localhost:9200/_cat/health

# Check connectivity from Product Service logs
# Look for "Elasticsearch" in service logs
```

### gRPC connection fails

```bash
# Check Product Service is running on port 9082
netstat -ano | findstr :9082

# Check Order Service logs for gRPC client errors
```

### RabbitMQ connectivity issues

```bash
# Check RabbitMQ is running
docker-compose -f docker-compose-local.yml logs rabbitmq

# Check management UI
curl -u guest:guest http://localhost:15672/api/overview

# Check queues are created
curl -u guest:guest http://localhost:15672/api/queues
```

### JWT Token validation fails

```bash
# Verify JWT_SECRET is set in all services
echo $env:JWT_SECRET

# Check token is being passed correctly
# Look for "JWT token validation failed" in logs
```

### Redis cache not working

```bash
# Test Redis connection
redis-cli ping

# Check Redis service
docker-compose -f docker-compose-local.yml logs redis
```

## 📊 API Reference

### Identity Service (Port 8081)

```
POST   /api/v1/auth/register        Register new user
POST   /api/v1/auth/login            Login and get JWT
POST   /api/v1/auth/refresh          Refresh JWT token
GET    /api/v1/users/{id}            Get user by ID
GET    /api/v1/auth/verify           Verify token
```

### Product Service (Port 8082)

```
GET    /api/v1/products              List products
POST   /api/v1/products              Create product
GET    /api/v1/products/{id}         Get product by ID
PUT    /api/v1/products/{id}         Update product
DELETE /api/v1/products/{id}         Delete product
GET    /api/v1/categories            List categories
GET    /api/v1/brands                List brands
```

### Order Service (Port 8083)

```
GET    /api/v1/orders                List orders
POST   /api/v1/orders                Create order
GET    /api/v1/orders/{id}           Get order by ID
PUT    /api/v1/orders/{id}           Update order
GET    /api/v1/cart                  Get cart
POST   /api/v1/cart/items            Add to cart
DELETE /api/v1/cart/items/{id}       Remove from cart
GET    /api/v1/inventory             Get inventory
```

### Payment Service (Port 8084)

```
GET    /api/v1/payments              List payments
POST   /api/v1/payments              Create payment
GET    /api/v1/payments/{id}         Get payment by ID
PUT    /api/v1/payments/{id}         Update payment status
```

### Notification Service (Port 8085)

```
GET    /api/v1/notifications         List notifications
POST   /api/v1/notifications         Create notification
GET    /api/v1/notifications/{id}    Get notification
DELETE /api/v1/notifications/{id}    Delete notification
```

### API Gateway (Port 8080)

All routes proxy to appropriate backend service:
```
/api/v1/auth/**          → Identity Service
/api/v1/users/**         → Identity Service
/api/v1/products/**      → Product Service
/api/v1/categories/**    → Product Service
/api/v1/brands/**        → Product Service
/api/v1/orders/**        → Order Service
/api/v1/cart/**          → Order Service
/api/v1/inventory/**     → Order Service
/api/v1/payments/**      → Payment Service
/api/v1/notifications/** → Notification Service
```

## 🧹 Cleanup

### Stop all services (keep Docker data):

```bash
# Stop Spring Boot services
# Just click Stop in IntelliJ for each service

# Stop Docker services
docker-compose -f docker-compose-local.yml stop
```

### Stop all services (remove Docker data):

```bash
# This will reset databases!
docker-compose -f docker-compose-local.yml down
docker volume rm project5_postgres_data project5_redis_data project5_rabbitmq_data project5_elasticsearch_data

# Restart services
docker-compose -f docker-compose-local.yml up -d
```

## 📚 Additional Resources

- `INTELLIJ_SETUP.md` - Detailed IntelliJ configuration
- `STARTUP_GUIDE.bat` - Batch file with startup instructions
- `COMPLETION_REPORT.md` - Full technical report
- Project source: `backend/` directory

## 🎯 Next Steps

1. **Develop Features**: Implement business logic in each service
2. **Write Tests**: Add unit and integration tests
3. **API Documentation**: Generate OpenAPI/Swagger docs
4. **Load Testing**: Test with realistic data volumes
5. **Deploy to VPS**: Use the same docker-compose.yml with updated env vars

---

**Last Updated**: 2026-10-11
**Project**: omnichannel-platform
**Environment**: Local Development (Windows)
