@echo off
REM Project5 Services Startup Script for Windows
REM This script helps startup all 6 services in the correct order

echo.
echo ============================================================
echo Project5 - Local Development Startup Guide
echo ============================================================
echo.
echo Prerequisites:
echo - Docker Desktop must be running with infrastructure services
echo - PostgreSQL, Redis, RabbitMQ, Elasticsearch must be healthy
echo - Gradle build must be completed
echo.
echo Services will be started in this order:
echo 1. Identity Service (port 8081)
echo 2. Product Service (port 8082)
echo 3. Order Service (port 8083)
echo 4. Payment Service (port 8084)
echo 5. Notification Service (port 8085)
echo 6. API Gateway (port 8080)
echo.
echo Open IntelliJ IDEA and follow these steps:
echo.
echo STEP 1: Infrastructure Services (Docker)
echo   docker-compose -f docker-compose-local.yml up -d
echo   Wait for all services to be healthy (check with: docker-compose -f docker-compose-local.yml ps)
echo.
echo STEP 2: Run Configurations
echo   - Go to Run menu -^> Edit Configurations
echo   - Add Spring Boot configurations for each service (6 total)
echo   - See INTELLIJ_SETUP.md for detailed instructions
echo   - Use these environment variables:
echo     * DB_HOST=localhost
echo     * REDIS_HOST=localhost
echo     * RABBITMQ_HOST=localhost
echo     * ELASTICSEARCH_URIS=http://localhost:9200
echo     * JWT_SECRET=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==
echo     * All service host/port variables set to localhost and their default ports
echo.
echo STEP 3: Start Services in IntelliJ
echo   - Each service has a run configuration created
echo   - Click the green Run button next to each configuration
echo   - Wait for startup logs: "Started ... in X.XXX seconds"
echo.
echo STEP 4: Test Services
echo   - Health check: curl http://localhost:8081/actuator/health
echo   - API Gateway: curl http://localhost:8080/api/v1/products
echo   - See TEST_ENDPOINTS.md for all test commands
echo.
echo MANUAL STARTUP COMMANDS (if not using IntelliJ GUI):
echo.
echo # Terminal 1 - Identity Service
echo cd d:\Project5
echo set DB_HOST=localhost
echo set DB_PORT=5432
echo set DB_USER=postgres
echo set DB_PASSWORD=123
echo set JWT_SECRET=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==
echo ./gradlew :backend:identity-service:bootRun --args="--spring.profiles.active=local"
echo.
echo # Terminal 2 - Product Service
echo cd d:\Project5
echo set DB_HOST=localhost
echo set ELASTICSEARCH_URIS=http://localhost:9200
echo set JWT_SECRET=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==
echo set GRPC_PORT=9082
echo ./gradlew :backend:product-service:bootRun --args="--spring.profiles.active=local"
echo.
echo # Terminal 3 - Order Service
echo cd d:\Project5
echo set DB_HOST=localhost
echo set REDIS_HOST=localhost
echo set RABBITMQ_HOST=localhost
echo set JWT_SECRET=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==
echo set GRPC_PRODUCT_HOST=localhost
echo set GRPC_PRODUCT_PORT=9082
echo ./gradlew :backend:order-service:bootRun --args="--spring.profiles.active=local"
echo.
echo # Terminal 4 - Payment Service
echo cd d:\Project5
echo set DB_HOST=localhost
echo set RABBITMQ_HOST=localhost
echo set JWT_SECRET=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==
echo ./gradlew :backend:payment-service:bootRun --args="--spring.profiles.active=local"
echo.
echo # Terminal 5 - Notification Service
echo cd d:\Project5
echo set DB_HOST=localhost
echo set RABBITMQ_HOST=localhost
echo ./gradlew :backend:notification-service:bootRun --args="--spring.profiles.active=local"
echo.
echo # Terminal 6 - API Gateway
echo cd d:\Project5
echo set REDIS_HOST=localhost
echo set JWT_SECRET=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==
echo ./gradlew :backend:api-gateway:bootRun --args="--spring.profiles.active=local"
echo.
echo ============================================================
echo Press any key to close this window...
pause
