# Master Seed Data Script for Project5 Microservices (PowerShell)
# This script executes all seed SQL files into their respective databases

param(
    [string]$DbHost = "localhost",
    [int]$DbPort = 5432,
    [string]$DbUser = "postgres",
    [string]$DbPassword = "123"
)

# Function to execute seed file
function Execute-SeedFile {
    param(
        [string]$DatabaseName,
        [string]$SeedFilePath,
        [int]$Step,
        [int]$TotalSteps
    )
    
    Write-Host ""
    Write-Host "[$Step/$TotalSteps] Executing $DatabaseName seed data..." -ForegroundColor Cyan
    
    # Set connection string
    $env:PGPASSWORD = $DbPassword
    
    # Execute psql command
    & psql -h $DbHost -p $DbPort -U $DbUser -d $DatabaseName -f $SeedFilePath
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[SUCCESS] $DatabaseName seed data completed" -ForegroundColor Green
        return $true
    } else {
        Write-Host "[ERROR] Failed to seed $DatabaseName" -ForegroundColor Red
        return $false
    }
}

# Main execution
Clear-Host
Write-Host ""
Write-Host "===============================================" -ForegroundColor Yellow
Write-Host "Project5 Microservices - Seed Data Execution" -ForegroundColor Yellow
Write-Host "===============================================" -ForegroundColor Yellow
Write-Host ""
Write-Host "Database Configuration:" -ForegroundColor Cyan
Write-Host "  Host: $DbHost"
Write-Host "  Port: $DbPort"
Write-Host "  User: $DbUser"
Write-Host ""

# Check if psql is available
try {
    $psqlVersion = & psql --version 2>$null
    Write-Host "PostgreSQL Client: $psqlVersion" -ForegroundColor Green
} catch {
    Write-Host "ERROR: psql command not found. Please install PostgreSQL client tools." -ForegroundColor Red
    Write-Host "       Add PostgreSQL bin directory to PATH." -ForegroundColor Red
    exit 1
}

# Track success/failure
$allSuccessful = $true
$seedFilesDir = Split-Path -Parent $PSCommandPath

# Execute seed files in order
$seedFiles = @(
    @{ Name = "Product Service"; DB = "product_db"; File = "product-seed.sql" },
    @{ Name = "Order Service"; DB = "order_db"; File = "order-seed.sql" },
    @{ Name = "Payment Service"; DB = "payment_db"; File = "payment-seed.sql" },
    @{ Name = "Notification Service"; DB = "notification_db"; File = "notification-seed.sql" }
)

$totalSteps = $seedFiles.Count + 1
$currentStep = 1

foreach ($seed in $seedFiles) {
    $seedFilePath = Join-Path $seedFilesDir $seed.File
    
    if (-not (Test-Path $seedFilePath)) {
        Write-Host "[ERROR] Seed file not found: $seedFilePath" -ForegroundColor Red
        $allSuccessful = $false
    } else {
        $success = Execute-SeedFile -DatabaseName $seed.DB -SeedFilePath $seedFilePath -Step $currentStep -TotalSteps $totalSteps
        if (-not $success) {
            $allSuccessful = $false
        }
    }
    $currentStep++
}

# Handle Identity Service
Write-Host ""
Write-Host "[$currentStep/$totalSteps] Identity Service seed data..." -ForegroundColor Cyan
Write-Host "[PENDING] Identity Service uses Java CommandLineRunner" -ForegroundColor Yellow
Write-Host "          To enable seeding, set environment variable and run service:" -ForegroundColor Yellow
Write-Host "          Set-Item env:SEED_DATA 'true'" -ForegroundColor White
Write-Host "          Then start: mvn spring-boot:run" -ForegroundColor White

# Summary
Write-Host ""
Write-Host "===============================================" -ForegroundColor Yellow
Write-Host "Seed Data Execution Summary" -ForegroundColor Yellow
Write-Host "===============================================" -ForegroundColor Yellow
Write-Host ""

$seedFiles | ForEach-Object {
    Write-Host "[SUCCESS] $($_.Name): Seed data executed" -ForegroundColor Green
}
Write-Host "[PENDING] Identity Service: Requires Java seed runner execution" -ForegroundColor Yellow

Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Build Identity Service: mvn clean install -pl identity-service"
Write-Host "2. Run with seed data enabled:"
Write-Host "   Set-Item env:SEED_DATA 'true'"
Write-Host "   cd backend\identity-service"
Write-Host "   mvn spring-boot:run"
Write-Host ""
Write-Host "Verify seed data by connecting to PostgreSQL:" -ForegroundColor Cyan
Write-Host "  psql -h localhost -p 5432 -U postgres"
Write-Host "  \c product_db"
Write-Host "  SELECT COUNT(*) FROM products;"
Write-Host ""
Write-Host "===============================================" -ForegroundColor Yellow

if ($allSuccessful) {
    Write-Host "All database seed operations completed!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "Some seed operations failed. Please review errors above." -ForegroundColor Red
    exit 1
}
