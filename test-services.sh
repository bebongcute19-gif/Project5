#!/usr/bin/env bash
# Test script for Project5 services

echo "=========================================="
echo "Testing Project5 Services"
echo "=========================================="
echo ""

# Test health endpoints
echo "1. Testing Health Endpoints"
echo "---"

services=(
    "Identity:8081"
    "Product:8082"
    "Order:8083"
    "Payment:8084"
    "Notification:8085"
    "Gateway:8080"
)

for service in "${services[@]}"; do
    IFS=':' read -r name port <<< "$service"
    echo -n "Testing $name (port $port)... "
    response=$(curl -s -w "\n%{http_code}" http://localhost:$port/actuator/health)
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | head -1)
    
    if [ "$http_code" = "200" ]; then
        echo "✓ OK"
        echo "  Status: $(echo $body | jq -r '.status' 2>/dev/null || echo 'N/A')"
    else
        echo "✗ FAILED (HTTP $http_code)"
    fi
done

echo ""
echo "2. Testing Identity Service - Register User"
echo "---"

register_response=$(curl -s -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }')

echo "Register Response:"
echo "$register_response" | jq . 2>/dev/null || echo "$register_response"

echo ""
echo "3. Testing Identity Service - Login"
echo "---"

login_response=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }')

echo "Login Response:"
echo "$login_response" | jq . 2>/dev/null || echo "$login_response"

# Extract token if login was successful
token=$(echo "$login_response" | jq -r '.token' 2>/dev/null)

if [ ! -z "$token" ] && [ "$token" != "null" ]; then
    echo ""
    echo "JWT Token obtained: ${token:0:50}..."
    
    echo ""
    echo "4. Testing Product Service - Get Products"
    echo "---"
    
    products=$(curl -s http://localhost:8082/api/v1/products \
      -H "Authorization: Bearer $token")
    
    echo "Products Response:"
    echo "$products" | jq . 2>/dev/null || echo "$products"
fi

echo ""
echo "5. Testing API Gateway Routes"
echo "---"

echo -n "Testing Gateway route to Product Service... "
gateway_response=$(curl -s -w "\n%{http_code}" http://localhost:8080/api/v1/products)
http_code=$(echo "$gateway_response" | tail -1)
body=$(echo "$gateway_response" | head -1)

if [ "$http_code" = "200" ] || [ "$http_code" = "401" ]; then
    echo "✓ Route working"
else
    echo "✗ Route failed (HTTP $http_code)"
fi

echo ""
echo "=========================================="
echo "Testing Complete"
echo "=========================================="
