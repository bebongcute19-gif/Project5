# System Architecture Specification — Omnichannel E-Commerce Platform (Project MD5)

## 1. Tổng quan Kiến trúc Microservices System Architecture

Hệ thống được xây dựng theo kiến trúc **Distributed Microservices** hoàn chỉnh, đảm bảo tính mô-đun hóa, khả năng mở rộng độc lập và chịu tải cao.

```mermaid
flowchart TB
    subgraph Clients["Clients & External Consumers"]
        WebApp["Web Client (React/Vue/NextJS)"]
        MobileApp["Mobile App (Flutter/React Native)"]
        ThirdParty["Third-Party Webhooks"]
    end

    subgraph EntryPoint["Edge & Gateway Layer"]
        NginxProxy["Nginx Reverse Proxy / SSL / UFW"]
        APIGateway["Spring Cloud Gateway (Port 8080)<br>• JWT Verification<br>• Rate Limiter (Redis)<br>• CORS & Request Routing"]
    end

    subgraph ServiceMesh["Microservices Layer (Docker Network)"]
        IdentitySvc["Identity Service (Port 8081)<br>• Spring Boot 3.x<br>• User Management, Auth, RBAC"]
        ProductSvc["Product Catalog Service (Port 8082)<br>• Spring Boot 3.x<br>• Products, SKUs, Search Engine"]
        OrderSvc["Order & Inventory Service (Port 8083)<br>• Spring Boot 3.x<br>• Order Saga, Atomic Inventory Reserve"]
        PaymentSvc["Payment Service (Port 8084)<br>• Spring Boot 3.x<br>• Payment Gateway Simulation"]
        NotiSvc["Notification Service (Port 8085)<br>• Spring Boot 3.x<br>• Async Email Notification Consumer"]
    end

    subgraph EventBroker["Message Broker Layer"]
        RabbitMQ["RabbitMQ Message Broker<br>• Exchanges: order, payment, notification<br>• Dead Letter Queues & Publisher Confirms"]
    end

    subgraph StorageLayer["Database Per Service Layer"]
        IdentityDB[("PostgreSQL 16<br>identity_db")]
        ProductDB[("MongoDB 7.0<br>product_db")]
        ESearch[("Elasticsearch 8.11<br>product_index")]
        OrderDB[("PostgreSQL 16<br>order_db")]
        PaymentDB[("PostgreSQL 16<br>payment_db")]
        NotiDB[("MongoDB 7.0<br>notification_db")]
        RedisCache[("Redis 7.2 Cluster<br>Rate Limit & Cache")]
    end

    subgraph Observability["Observability & Telemetry Layer"]
        Prometheus["Prometheus Metrics"]
        Grafana["Grafana Dashboards"]
        Loki["Loki Log Aggregator"]
        Jaeger["Jaeger Distributed Tracing"]
    end

    %% Edge Connections
    WebApp -->|HTTPS| NginxProxy
    MobileApp -->|HTTPS| NginxProxy
    ThirdParty -->|HTTPS| NginxProxy
    NginxProxy -->|HTTP| APIGateway

    %% Gateway Routing
    APIGateway -->|REST| IdentitySvc
    APIGateway -->|REST| ProductSvc
    APIGateway -->|REST| OrderSvc
    APIGateway -->|REST| PaymentSvc

    %% Internal Synchronous Communication
    OrderSvc ==>|gRPC HTTP/2 Latency < 5ms| ProductSvc
    APIGateway -.->|Rate Limit Check| RedisCache

    %% Databases Connections
    IdentitySvc ---> IdentityDB
    ProductSvc ---> ProductDB
    ProductSvc ---> ESearch
    OrderSvc ---> OrderDB
    OrderSvc -.-> RedisCache
    PaymentSvc ---> PaymentDB
    NotiSvc ---> NotiDB

    %% Event-Driven Connections
    OrderSvc -->|Publish Events / Outbox| RabbitMQ
    PaymentSvc -->|Publish Events / Outbox| RabbitMQ
    RabbitMQ -->|Consume Events| NotiSvc
    RabbitMQ -->|Consume Events| OrderSvc
    RabbitMQ -->|Consume Events| PaymentSvc

    %% Observability Connections
    ServiceMesh -.->|Actuator Metrics| Prometheus
    ServiceMesh -.->|Logs| Loki
    ServiceMesh -.->|OpenTelemetry Traces| Jaeger
    Prometheus --> Grafana
    Loki --> Grafana
```

---

## 2. API Gateway Routing Architecture

API Gateway (`Spring Cloud Gateway`) hoạt động như Single Point of Entry duy nhất cho hệ thống.

```mermaid
flowchart LR
    subgraph IncomingRequest["Client Request"]
        REQ["HTTP / HTTPS Request"]
    end

    subgraph GatewayFilters["Spring Cloud Gateway Filters"]
        GlobalFilter["Global CORS & Logging Filter"]
        RateLimitFilter["Redis Rate Limiter Filter"]
        JwtFilter["JWT Authentication Verification Filter"]
        RoutingFilter["Dynamic Path Routing Filter"]
    end

    subgraph Services["Downstream Services"]
        ID_SVC["Identity Service (/api/v1/auth/**)"]
        PR_SVC["Product Service (/api/v1/products/**)"]
        OR_SVC["Order Service (/api/v1/orders/**)"]
        PA_SVC["Payment Service (/api/v1/payments/**)"]
    end

    REQ --> GlobalFilter
    GlobalFilter --> RateLimitFilter
    RateLimitFilter -->|Allowed| JwtFilter
    RateLimitFilter -->|Exceeded| ERR_429["429 Too Many Requests"]
    JwtFilter -->|Valid JWT / Public Route| RoutingFilter
    JwtFilter -->|Invalid Token| ERR_401["401 Unauthorized"]
    
    RoutingFilter -->|Prefix /api/v1/auth| ID_SVC
    RoutingFilter -->|Prefix /api/v1/products| PR_SVC
    RoutingFilter -->|Prefix /api/v1/orders| OR_SVC
    RoutingFilter -->|Prefix /api/v1/payments| PA_SVC
```

---

## 3. Database Architecture (Database Per Service)

Kiến trúc đảm bảo mỗi Microservice làm chủ hoàn toàn dữ liệu của mình:

| Service | Database Engine | Loại hình Dữ liệu | Lý do lựa chọn |
| :--- | :--- | :--- | :--- |
| **Identity Service** | PostgreSQL 16 | Relational SQL | Đảm bảo tính toàn vẹn quan hệ (Users, Roles, Addresses), ACID transactions. |
| **Product Catalog Service** | MongoDB 7.0 + Elasticsearch 8 | Document NoSQL + Search Engine | Schema linh hoạt cho thuộc tính sản phẩm (Attributes, Variants), Elasticsearch cho full-text search. |
| **Order & Inventory Service** | PostgreSQL 16 + Redis 7.2 | Relational SQL + In-Memory | ACID cao cho đơn hàng, PostgreSQL Atomic Update cho tồn kho, Redis cho Distributed Lock. |
| **Payment Service** | PostgreSQL 16 | Relational SQL | Đảm bảo độ chính xác tài chính 100%, Audit log giao dịch khắt khe. |
| **Notification Service** | MongoDB 7.0 | Document NoSQL | Lưu trữ Log gửi email linh hoạt dạng JSON document. |
