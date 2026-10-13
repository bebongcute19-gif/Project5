# Test script for Project5 services on Windows

Write-Host "=========================================="
Write-Host "Testing Project5 Services"
Write-Host "=========================================="
Write-Host ""

# Test health endpoints
Write-Host "1. Testing Health Endpoints"
Write-Host "---"

$services = @{
    'Identity' = 8081
    'Product' = 8082
    'Order' = 8083
    'Payment' = 8084
    'Notification' = 8085
    'Gateway' = 8080
}

foreach ($service in $services.GetEnumerator()) {
    $name = $service.Name
    $port = $service.Value
    
    Write-Host -NoNewline "Testing $name (port $port)... "
    
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$port/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        $body = $response.Content | ConvertFrom-Json
        Write-Host "✓ OK (Status: $($body.status))"
    } catch {
        Write-Host "✗ FAILED - $($_.Exception.Message)"
    }
}

Write-Host ""
Write-Host "2. Testing Identity Service - Register User"
Write-Host "---"

$registerBody = @{
    username = "testuser"
    email = "test@example.com"
    password = "password123"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/v1/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $registerBody `
        -UseBasicParsing `
        -TimeoutSec 5 `
        -ErrorAction Stop
    
    $registerData = $registerResponse.Content | ConvertFrom-Json
    Write-Host "Register Response:"
    Write-Host ($registerData | ConvertTo-Json)
} catch {
    Write-Host "Register Error: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "3. Testing Identity Service - Login"
Write-Host "---"

$loginBody = @{
    username = "testuser"
    password = "password123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/v1/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody `
        -UseBasicParsing `
        -TimeoutSec 5 `
        -ErrorAction Stop
    
    $loginData = $loginResponse.Content | ConvertFrom-Json
    Write-Host "Login Response:"
    Write-Host ($loginData | ConvertTo-Json)
    
    $token = $loginData.token
    
    if ($token) {
        Write-Host ""
        Write-Host "JWT Token obtained: $($token.Substring(0, [Math]::Min(50, $token.Length)))..."
        
        Write-Host ""
        Write-Host "4. Testing Product Service - Get Products"
        Write-Host "---"
        
        try {
            $productResponse = Invoke-WebRequest -Uri "http://localhost:8082/api/v1/products" `
                -Headers @{ "Authorization" = "Bearer $token" } `
                -UseBasicParsing `
                -TimeoutSec 5 `
                -ErrorAction Stop
            
            $productData = $productResponse.Content | ConvertFrom-Json
            Write-Host "Products Response:"
            Write-Host ($productData | ConvertTo-Json -Depth 3)
        } catch {
            Write-Host "Product Error: $($_.Exception.Message)"
        }
    }
} catch {
    Write-Host "Login Error: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "5. Testing API Gateway Routes"
Write-Host "---"

Write-Host -NoNewline "Testing Gateway route to Product Service... "
try {
    $gatewayResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/products" `
        -UseBasicParsing `
        -TimeoutSec 5 `
        -ErrorAction Stop
    
    Write-Host "✓ Route working"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    if ($statusCode -eq 401 -or $statusCode -eq 403) {
        Write-Host "✓ Route working (requires auth)"
    } else {
        Write-Host "✗ Route failed - $($_.Exception.Message)"
    }
}

Write-Host ""
Write-Host "=========================================="
Write-Host "Testing Complete"
Write-Host "=========================================="
