# Implementation Plan — Omnichannel E-Commerce Platform (Project MD5)

## Overview
Dự án **Omnichannel E-Commerce Platform** được thiết kế theo kiến trúc Microservices chuẩn doanh nghiệp, hướng tới khả năng mở rộng (scalability), chịu tải cao (Flash Sale scenarios), đảm bảo tính nhất quán dữ liệu (Data Consistency via Saga & Transactional Outbox) và sẵn sàng triển khai trên môi trường Production Ubuntu VPS.

---

## Giai đoạn 1: Phân tích & Thiết kế Kiến trúc (Phase 1 — Analysis & Design)

### Task 1: Phân tích Yêu cầu (Requirements Analysis)
- [x] Phân tích Functional Requirements (FR) và Non-Functional Requirements (NFR).
- [x] Phân loại yêu cầu bắt buộc (Must-Have) và mở rộng (Extended/Nice-to-Have).
- [x] Xây dựng Requirement Traceability Matrix (RTM).
- [x] Định nghĩa chi tiết Acceptance Criteria (AC) cho từng tính năng.
- **File đầu ra:** `docs/requirements.md`, `docs/testing/acceptance-criteria.md`

### Task 2: Domain Driven Design (DDD)
- [x] Xác định Bounded Context cho 5 microservices (Identity, Product Catalog, Order & Inventory, Payment, Notification).
- [x] Thiết kế Entities, Aggregates, Value Objects, Domain Events cho từng context.
- [x] Thiết kế Business Rules & Invariants.
- [x] Thiết kế luồng giao tiếp giữa các services (Đồng bộ qua REST/gRPC và Bất đồng bộ qua RabbitMQ).
- **File đầu ra:** `docs/architecture/bounded-contexts.md`

### Task 3: Architecture & System Diagrams
- [x] Tổng quan Microservices Architecture Diagram (Mermaid).
- [x] API Gateway Routing Diagram.
- [x] REST & gRPC Communication Diagram.
- [x] RabbitMQ Event Flow Diagram.
- [x] Database Architecture Diagram.
- [x] Docker & Deployment Architecture Diagram.
- **File đầu ra:** `docs/architecture/system-architecture.md`, `docs/architecture/communication-flow.md`, `docs/architecture/deployment-architecture.md`

### Task 4: Database Design
- [x] Thiết kế PostgreSQL Schema cho `Identity Service` (users, roles, permissions, refresh_tokens, addresses).
- [x] Thiết kế MongoDB & Elasticsearch Schema cho `Product Catalog Service` (products, categories, brands, product_variants/SKUs).
- [x] Thiết kế PostgreSQL Schema cho `Order & Inventory Service` (orders, order_items, inventory, stock_reservations, outbox_events, processed_events).
- [x] Thiết kế PostgreSQL Schema cho `Payment Service` (payments, payment_transactions, outbox_events, processed_events).
- [x] Thiết kế MongoDB & PostgreSQL Schema cho `Notification Service` (notification_logs, processed_events).
- [x] Định nghĩa Primary Keys, Foreign Keys, Indexes, Constraints, và Isolation levels.
- **File đầu ra:** `docs/database/identity-schema.md`, `docs/database/product-schema.md`, `docs/database/order-schema.md`, `docs/database/payment-schema.md`, `docs/database/notification-schema.md`

### Task 5: API-First Design (OpenAPI 3.0 Specs)
- [x] API Gateway & Identity Service OpenAPI Spec (`docs/api/identity-openapi.yaml`).
- [x] Product Catalog Service OpenAPI Spec (`docs/api/product-openapi.yaml`).
- [x] Order & Inventory Service OpenAPI Spec (`docs/api/order-openapi.yaml`).
- [x] Payment Service OpenAPI Spec (`docs/api/payment-openapi.yaml`).
- [x] Notification Service OpenAPI Spec (`docs/api/notification-openapi.yaml`).
- [x] Quy chuẩn chung: Request/Response DTOs, Validations, Error Format (RFC 7807 Problem Details), Pagination/Filtering/Sorting, Idempotency-Key.

### Task 6: Distributed Transactions (Saga & Outbox Pattern)
- [x] Thiết kế Orchestrated Checkout Saga State Machine.
- [x] Thiết kế Compensation Transactions (Hoàn tác tồn kho, hủy đơn hàng, refund thanh toán).
- [x] Thiết kế Transactional Outbox Pattern & Idempotent Consumer pattern (`processed_events`).
- [x] Thiết kế Event Catalog & RabbitMQ Topology (Exchanges, Queues, Dead Letter Queues, TTL, Retries).
- **File đầu ra:** `docs/workflows/checkout-saga.md`, `docs/workflows/outbox-pattern.md`, `docs/events/event-catalog.md`, `docs/events/rabbitmq-topology.md`

### Task 7: Concurrency & Race Condition Control (High-Scale Inventory)
- [x] Mô hình hóa kịch bản Flash Sale: 100 concurrent requests tranh mua 1 item còn lại.
- [x] So sánh Atomic Conditional Update (`UPDATE ... WHERE quantity >= qty`) vs Row-Level Locking (`SELECT FOR UPDATE`).
- [x] Thiết kế cơ chế đảm bảo Zero Overselling & Idempotency.
- [x] Định nghĩa vai trò chính xác của Redis (L2 Cache / Rate Limit, không phải SSOT cho tồn kho).
- **File đầu ra:** `docs/workflows/inventory-concurrency.md`

### Task 8: DevOps & Observability Architecture
- [x] Thiết kế Docker Multi-stage Builds cho Spring Boot services.
- [x] Thiết kế Docker Compose cho Local Dev (`docker-compose.yml`) & Production Server (`docker-compose.prod.yml`).
- [x] Thiết kế CI/CD Pipeline với GitHub Actions & GHCR.
- [x] Thiết kế Nginx Reverse Proxy, SSL/TLS, UFW Firewall rules trên VPS Ubuntu.
- [x] Thiết kế Observability Stack: Prometheus (Metrics), Grafana (Dashboards), Loki (Logs), Jaeger (Distributed Tracing).
- [x] Thiết kế Chiến lược Testing (JUnit 5, Testcontainers, k6 scenarios).
- **File đầu ra:** `docs/architecture/deployment-architecture.md`, `docs/testing/test-strategy.md`

---

## Giai đoạn 2: Triển khai Code & Hạ tầng (Phase 2 — Implementation & Infrastructure)
*(Sẽ được thực hiện sau khi kết thúc Giai đoạn 1 và có sự chấp thuận của User)*
1. Khởi tạo Spring Boot Microservices Projects (Gradle Multi-Module / Independent Repositories).
2. Triển khai API Gateway, JWT Filter, Rate Limiter.
3. Triển khai Core Business Services (Identity, Product, Order, Payment, Notification).
4. Triển khai gRPC communication giữa Order Service và Product Service.
5. Triển khai RabbitMQ Event Producer/Consumer + Outbox Scheduler.
6. Triển khai Integration Tests với Testcontainers & k6 load tests.
7. Triển khai CI/CD pipeline và deploy trên Ubuntu VPS với Monitoring stack.
