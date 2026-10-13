@echo off
REM Master Seed Data Script for Project5 Microservices
REM This script executes all seed SQL files into their respective databases

setlocal enabledelayedexpansion

REM Database configuration (from application-local.yml)
set DB_HOST=localhost
set DB_PORT=5432
set DB_USER=postgres
set DB_PASSWORD=123

REM Colors for output
for /F %%A in ('echo prompt $H^| cmd') do set "BS=%%A"

echo.
echo ===============================================
echo Project5 Microservices - Seed Data Execution
echo ===============================================
echo.
echo Database Configuration:
echo   Host: %DB_HOST%
echo   Port: %DB_PORT%
echo   User: %DB_USER%
echo.

REM Check if psql is available
where psql >nul 2>nul
if errorlevel 1 (
    echo ERROR: psql command not found. Please install PostgreSQL client tools.
    echo        Add PostgreSQL bin directory to PATH.
    exit /b 1
)

REM Function to execute seed file
setlocal enabledelayedexpansion

REM Execute Product Service Seed Data
echo.
echo [1/5] Executing Product Service seed data...
set "PGPASSWORD=%DB_PASSWORD%"
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d product_db -f "%~dp0product-seed.sql"
if errorlevel 1 (
    echo ERROR: Failed to seed product_db
    endlocal
    exit /b 1
) else (
    echo SUCCESS: Product Service seed data completed
)

REM Execute Order Service Seed Data
echo.
echo [2/5] Executing Order Service seed data...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d order_db -f "%~dp0order-seed.sql"
if errorlevel 1 (
    echo ERROR: Failed to seed order_db
    endlocal
    exit /b 1
) else (
    echo SUCCESS: Order Service seed data completed
)

REM Execute Payment Service Seed Data
echo.
echo [3/5] Executing Payment Service seed data...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d payment_db -f "%~dp0payment-seed.sql"
if errorlevel 1 (
    echo ERROR: Failed to seed payment_db
    endlocal
    exit /b 1
) else (
    echo SUCCESS: Payment Service seed data completed
)

REM Execute Notification Service Seed Data
echo.
echo [4/5] Executing Notification Service seed data...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d notification_db -f "%~dp0notification-seed.sql"
if errorlevel 1 (
    echo ERROR: Failed to seed notification_db
    endlocal
    exit /b 1
) else (
    echo SUCCESS: Notification Service seed data completed
)

REM Note: Identity Service uses Java seed runner
echo.
echo [5/5] Identity Service seed data...
echo NOTE: Identity Service uses Java CommandLineRunner for proper password encoding
echo       To enable seeding, run identity service with environment variable:
echo       SET SEED_DATA=true
echo       Then start the service: mvn spring-boot:run
echo.

REM Summary
echo.
echo ===============================================
echo Seed Data Execution Summary
echo ===============================================
echo.
echo [SUCCESS] Product Service:     Seed data executed
echo [SUCCESS] Order Service:        Seed data executed
echo [SUCCESS] Payment Service:      Seed data executed
echo [SUCCESS] Notification Service: Seed data executed
echo [PENDING] Identity Service:     Requires Java seed runner execution
echo.
echo Next Steps:
echo 1. Build Identity Service: mvn clean install -pl identity-service
echo 2. Run with seed data enabled:
echo    SET SEED_DATA=true
echo    cd backend\identity-service
echo    mvn spring-boot:run
echo.
echo Verify seed data by connecting to PostgreSQL:
echo   psql -h localhost -p 5432 -U postgres
echo   \c product_db
echo   SELECT COUNT(*) FROM products;
echo.
echo ===============================================

endlocal
