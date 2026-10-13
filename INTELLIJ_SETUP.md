# IntelliJ IDEA Run Configuration Guide

## Environment Variables (shared for all 6 services)

Set these in IntelliJ Run Configuration > Environment variables:

```
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=123
REDIS_HOST=localhost
REDIS_PORT=6379
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
ELASTICSEARCH_URIS=http://localhost:9200
JWT_SECRET=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==
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
GRPC_PRODUCT_HOST=localhost
GRPC_PRODUCT_PORT=9082
```

## Run Configuration Steps

### For Each Service:

1. **Click Run > Edit Configurations...**
2. **Click + (Add New Configuration)**
3. **Select Spring Boot**
4. **Fill in the following:**

| Service | Configuration |
|---------|---|
| Identity Service | Name: `Identity Service`<br/>Main class: `com.omnichannel.identity.IdentityServiceApplication`<br/>Module: `backend:identity-service`<br/>Program arguments: `--spring.profiles.active=local`<br/>VM options: (leave empty) |
| Product Service | Name: `Product Service`<br/>Main class: `com.omnichannel.product.ProductServiceApplication`<br/>Module: `backend:product-service`<br/>Program arguments: `--spring.profiles.active=local`<br/>VM options: (leave empty) |
| Order Service | Name: `Order Service`<br/>Main class: `com.omnichannel.order.OrderServiceApplication`<br/>Module: `backend:order-service`<br/>Program arguments: `--spring.profiles.active=local`<br/>VM options: (leave empty) |
| Payment Service | Name: `Payment Service`<br/>Main class: `com.omnichannel.payment.PaymentServiceApplication`<br/>Module: `backend:payment-service`<br/>Program arguments: `--spring.profiles.active=local`<br/>VM options: (leave empty) |
| Notification Service | Name: `Notification Service`<br/>Main class: `com.omnichannel.notification.NotificationServiceApplication`<br/>Module: `backend:notification-service`<br/>Program arguments: `--spring.profiles.active=local`<br/>VM options: (leave empty) |
| API Gateway | Name: `API Gateway`<br/>Main class: `com.omnichannel.gateway.ApiGatewayApplication`<br/>Module: `backend:api-gateway`<br/>Program arguments: `--spring.profiles.active=local`<br/>VM options: (leave empty) |

5. **Tab "Environment variables":**
   - Copy and paste all environment variables from above section
   
6. **Tab "Before launch":**
   - Add build task: `Gradle-aware Make`
   - Module: `backend:<service-name>`

7. **Click OK**

## Startup Order

Start services in this order:

1. **Identity Service** (port 8081) - no dependencies on other services
2. **Product Service** (port 8082) - depends on Elasticsearch (already running in Docker)
3. **Order Service** (port 8083) - depends on Product Service (gRPC), Redis, RabbitMQ
4. **Payment Service** (port 8084) - depends on RabbitMQ
5. **Notification Service** (port 8085) - depends on RabbitMQ
6. **API Gateway** (port 8080) - depends on all other services

## Gradle Build Setup

### Build All:
```bash
cd d:\Project5
./gradlew build
```

### Build Individual Service:
```bash
./gradlew :backend:identity-service:build
./gradlew :backend:product-service:build
./gradlew :backend:order-service:build
./gradlew :backend:payment-service:build
./gradlew :backend:notification-service:build
./gradlew :backend:api-gateway:build
```

## Expected Main Class Names

If the main classes are different, you can find them by:

1. Go to each service's src/main/java folder
2. Look for a class with @SpringBootApplication annotation
3. Usually named `*Application.java`

Common patterns:
- `IdentityServiceApplication`
- `ProductServiceApplication`
- `OrderServiceApplication`
- `PaymentServiceApplication`
- `NotificationServiceApplication`
- `ApiGatewayApplication`

## Troubleshooting

### "Cannot find module"
- Make sure project is opened correctly in IntelliJ
- File > Invalidate Caches > Invalidate and Restart

### "No Spring Boot Application main class"
- Right-click service folder > Mark Directory as > Sources Root
- Find the main application class in src/main/java

### "Port already in use"
- Check if service is already running
- Use: `netstat -ano | findstr :8081` (replace 8081 with port)
- Kill process: `taskkill /PID <PID> /F`

### Database migration fails
- Ensure PostgreSQL is running and healthy
- Check logs for exact error
- Database should be created by docker-compose

### gRPC connection fails
- Ensure Product Service is running on port 9082
- Check if Order Service can resolve localhost:9082
- May need to rebuild protobuf: `./gradlew :backend:product-service:generateProto`

### Elasticsearch connection fails
- Ensure Elasticsearch is running on port 9200
- Test: `curl http://localhost:9200/_cat/health`
- Check logs for detailed errors

## After All Services Start

### Verify Health:
```bash
curl http://localhost:8081/actuator/health  # Identity
curl http://localhost:8082/actuator/health  # Product
curl http://localhost:8083/actuator/health  # Order
curl http://localhost:8084/actuator/health  # Payment
curl http://localhost:8085/actuator/health  # Notification
curl http://localhost:8080/actuator/health  # API Gateway
```

### Test Login:
```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

### Test Product API:
```bash
curl http://localhost:8080/api/v1/products
```
