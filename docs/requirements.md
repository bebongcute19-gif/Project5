# Requirements Specification — Omnichannel E-Commerce Platform (Project MD5)

## 1. Giới thiệu tổng quan
Tài liệu này xác định các yêu cầu chức năng (Functional Requirements - FR), yêu cầu phi chức năng (Non-Functional Requirements - NFR), phân loại mức độ ưu tiên, ma trận truy xuất nguồn gốc yêu cầu (Requirement Traceability Matrix - RTM) cho hệ thống **Omnichannel E-Commerce Platform (Project MD5)**.

---

## 2. Technical Stack & Dependencies Justification

| Thành phần | Công nghệ chọn lựa | Phiên bản đề xuất | Lý do chọn lựa & Tính ổn định |
| :--- | :--- | :--- | :--- |
| **Language** | Java | 17 LTS | Phiên bản Long Term Support, tối ưu hóa JVM performance, hỗ trợ Sealed Classes, Records, Pattern Matching. |
| **Framework** | Spring Boot | 3.2.x | Tối ưu tương thích với Java 17, hỗ trợ Observability (Micrometer, Otlp), Spring Security 6.x hiện đại. |
| **Microservices** | Spring Cloud | 2023.0.x (Leyton) | Bộ giải pháp hoàn chỉnh cho Service Registry, Gateway, Distributed Tracing và Resilience4j integration. |
| **API Gateway** | Spring Cloud Gateway | 4.1.x | Reactive (Spring WebFlux / Netty), khả năng xử lý hàng chục nghìn rps với mượt mà, dễ viết Custom Filters. |
| **Security** | Spring Security + JWT | 6.2.x + jjwt 0.12.x | Chuẩn xác thực phân tán mượt mà, Stateless JWT, hỗ trợ HMAC-SHA256 / RSA Key pair. |
| **Build Tool** | Gradle | 8.5 | Tốc độ build nhanh với Incremental Build & Build Cache, khai báo Kotlin/Groovy DSL rõ ràng. |
| **Databases** | PostgreSQL<br>MongoDB | 16.x<br>7.0.x | PostgreSQL cho ACID Transactions (Identity, Order, Payment). MongoDB cho Flexible Document Schema (Product Catalog, Notifications). |
| **Cache & Lock** | Redis | 7.2.x | In-memory key-value store cho Rate Limiting, Distributed Locking (Redisson), Session Cache. |
| **Search Engine** | Elasticsearch | 8.11.x | Full-text search siêu tốc cho danh mục sản phẩm, hỗ trợ Fuzzy search, Auto-complete, Aggregation filtering. |
| **Message Broker**| RabbitMQ | 3.12.x | AMQP Protocol chuẩn, hỗ trợ Reliable Messaging (Publisher Confirms, Manual ACK, Dead Letter Exchange), tương thích cao với Spring AMQP. |
| **Internal RPC** | gRPC / Protocol Buffers | 1.60.x | Giao tiếp nội bộ Order -> Product siêu nhanh qua HTTP/2, Protobuf payload nhỏ gọn, Latency < 5ms. |
| **Containerization**| Docker / Compose | 24.0.x / 2.23.x | Đóng gói môi trường chuẩn xác giữa Local và VPS Production. Multi-stage build tối ưu size image. |
| **CI/CD & Registry**| GitHub Actions + GHCR | Standard | Tự động hóa Lint, Build, Test, Build Docker Image, Push GHCR và Continuous Deployment qua SSH/Docker Compose. |
| **Monitoring** | Prometheus + Grafana + Loki + Jaeger | Latest Stable | Bộ tứ Observability hoàn chỉnh: Metrics (Prometheus), Visuals (Grafana), Log Centralized (Loki), Tracing (Jaeger). |
| **Testing** | JUnit 5 + Testcontainers + k6 | 5.10 / 1.19 / 0.47 | JUnit 5 cho Unit/Integration, Testcontainers khởi tạo real DB/RabbitMQ ngẫu nhiên khi chạy test, k6 cho stress/load testing. |

---

## 3. Functional Requirements (FR)

### 3.1. Identity & Access Management Service (Port 8081)
- **FR-ID-01 (User Registration):** Cho phép người dùng đăng ký tài khoản mới với Email, Password, Full Name, Phone. Mã hóa password bằng BCrypt.
- **FR-ID-02 (User Authentication):** Đăng nhập bằng Email/Password, trả về Access Token (JWT, TTL ngắn 15m) và Refresh Token (Opaque/JWT, TTL dài 7d).
- **FR-ID-03 (Token Management):** API Refresh Token để cấp lại Access Token mới mà không cần re-login. Hỗ trợ Revoke Token khi Logout.
- **FR-ID-04 (Role-Based Access Control - RBAC):** Quản lý Roles (`ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_STAFF`) và Permissions.
- **FR-ID-05 (User Profile & Address):** Người dùng có thể xem/cập nhật Profile và quản lý danh sách địa chỉ nhận hàng (Set default address).

### 3.2. Product Catalog & Search Service (Port 8082)
- **FR-PR-01 (Category Management):** Quản lý danh mục sản phẩm theo cấp cha-con (Tree structure).
- **FR-PR-02 (Brand Management):** Quản lý thương hiệu sản phẩm.
- **FR-PR-03 (Product & SKU Management):** CRUD Sản phẩm với các thuộc tính động (Color, Size, Spec), quản lý nhiều biến thể (SKUs) có mã SKU riêng, giá bán, giá khuyến mãi.
- **FR-PR-04 (Search & Filter):** Tìm kiếm sản phẩm full-text bằng Elasticsearch (theo tên, mô tả), bộ lọc theo danh mục, thương hiệu, khoảng giá, sắp xếp theo giá/ngày tạo.
- **FR-PR-05 (gRPC Query Service):** Cung cấp gRPC Endpoint cho `Order Service` truy vấn tính hợp lệ và thông tin chi tiết của danh sách SKUs.

### 3.3. Order & Inventory Service (Port 8083)
- **FR-OR-01 (Inventory Management):** Quản lý số lượng tồn kho khả dụng (Available Stock) và tồn kho tạm giữ (Reserved Stock) theo SKU.
- **FR-OR-02 (Order Creation & Stock Reservation):** Khách hàng tạo đơn hàng. Hệ thống kiểm tra và tạm giữ kho (Stock Reservation) trong 15 phút.
- **FR-OR-03 (Saga Orchestration):** Điêu phối quy trình Checkout Saga: Khởi tạo đơn (PENDING) -> Tạm giữ kho (RESERVED) -> Gọi Payment Service -> Đơn hàng hoàn tất (CONFIRMED) hoặc Thất bại (CANCELLED).
- **FR-OR-04 (Order State Machine):** Quản lý trạng thái đơn hàng: `PENDING_PAYMENT` -> `CONFIRMED` -> `SHIPPING` -> `COMPLETED` -> `CANCELLED`.
- **FR-OR-05 (Inventory Concurrency Control):** Đảm bảo tuyệt đối không bị Overselling (bán quá số lượng tồn) khi có hàng trăm request đồng thời tranh mua cùng 1 SKU.

### 3.4. Payment Service (Port 8084)
- **FR-PA-01 (Payment Simulation):** Mô phỏng thanh toán trực tuyến (MOCK Payment Gateway / VNPay / MoMo simulation) với kết quả Thành công / Thất bại.
- **FR-PA-02 (Payment Transaction Record):** Ghi nhận lịch sử giao dịch thanh toán, mã giao dịch (Transaction Reference), số tiền, phương thức thanh toán.
- **FR-PA-03 (Payment Events):** Phát sự kiện `PaymentSucceededEvent` hoặc `PaymentFailedEvent` lên RabbitMQ sau khi giao dịch hoàn tất.
- **FR-PA-04 (Payment Refund):** Hỗ trợ xử lý hoàn tiền (Refund transaction) khi Saga thực hiện bước bồi thường (Compensation).

### 3.5. Notification Service (Port 8085)
- **FR-NO-01 (Email Notification):** Tự động gửi Email xác nhận đơn hàng, email thông báo thanh toán thành công/thất bại, email cập nhật vận chuyển.
- **FR-NO-02 (Message Consumer):** Lắng nghe các sự kiện từ RabbitMQ (`OrderCreatedEvent`, `PaymentSucceededEvent`, `OrderCancelledEvent`).
- **FR-NO-03 (Notification Log & Idempotency):** Ghi nhật ký gửi mail vào DB, đảm bảo idempotency không gửi mail trùng lặp khi event bị retry.

### 3.6. API Gateway (Port 8080)
- **FR-GW-01 (Dynamic Routing):** Điều hướng request đến đúng Microservice theo prefix đường dẫn (`/api/v1/auth/**`, `/api/v1/products/**`, v.v.).
- **FR-GW-02 (Centralized JWT Verification):** Kiểm tra tính hợp lệ của JWT Header (`Authorization: Bearer <token>`), trích xuất `userId`, `roles` và inject vào Request Headers gửi tới downstream services.
- **FR-GW-03 (Rate Limiting):** Giới hạn số lượng request per IP/User bằng Redis Rate Limiter để chống DDOS và Spam API.
- **FR-GW-04 (CORS Handling):** Cấu hình CORS tập trung cho phép Web/Mobile Client truy cập an toàn.

---

## 4. Non-Functional Requirements (NFR)

- **NFR-01 (Performance & Latency):**
  - Read APIs (Danh sách sản phẩm, chi tiết sản phẩm): Latency P95 < 100ms với 1,000 RPS.
  - Checkout API (Order Creation & Reservation): Latency P95 < 300ms.
  - Internal gRPC Call (Order -> Product): Latency P99 < 5ms.
- **NFR-02 (Scalability & Concurrency):**
  - Hệ thống chịu được tải 100 concurrent requests tranh mua 1 item duy nhất mà không bị rò rỉ dữ liệu hoặc sai lệch tồn kho.
  - Mọi microservice thiết kế dạng Stateless, sẵn sàng scale out chiều ngang (Horizontal Scaling) lên N instances.
- **NFR-03 (Data Consistency & Reliability):**
  - Đảm bảo tính Eventual Consistency bằng Saga Pattern và Transactional Outbox Pattern.
  - At-Least-Once Delivery cho Domain Events, không bị mất sự kiện khi RabbitMQ bận hoặc restart.
- **NFR-04 (Security & Compliance):**
  - Mật khẩu mã hóa BCrypt với Strength 12.
  - HTTPS/TLS 1.3 cho toàn bộ kết nối bên ngoài. Internal communication nằm trong Docker Network an toàn.
  - Không lộ thông tin nhạy cảm trong Log (Masking Password, Credit Card token).
- **NFR-05 (Observability & Maintainability):**
  - Mọi log đều có `traceId` và `spanId` (Distributed Tracing với Jaeger/Zipkin).
  - Prometheus thu thập metrics `/actuator/prometheus` từ tất cả services.
- **NFR-06 (Availability):** Target Uptime 99.9% cho các service trọng yếu.

---

## 5. Phân loại Yêu cầu (Must-Have vs Extended)

### 5.1. Bắt buộc (Must-Have — Phase 1 Design & Phase 2 Implementation)
1. Tách biệt 5 Microservices + API Gateway với DB riêng biệt.
2. JWT Authentication, Authorization & Address Management.
3. Product Catalog (PostgreSQL/MongoDB) + Elasticsearch Full-text Search.
4. Order Creation, Inventory Stock Reservation & Atomic Concurrency Update.
5. Saga Orchestration cho quy trình Checkout (Pending -> Stock Reserved -> Payment -> Confirmed/Cancelled).
6. Payment Simulation & Webhook handling.
7. Notification Service gửi mail bất đồng bộ qua RabbitMQ.
8. Transactional Outbox Pattern & Idempotent Consumer.
9. Docker Compose & Multi-stage Dockerfile cho local/production dev.
10. OpenAPI 3.0 Documentation cho tất cả API endpoints.

### 5.2. Mở rộng (Extended / Nice-To-Have)
1. Resilience4j Circuit Breaker tại API Gateway & Order Service.
2. Redis Distributed Cache cho Product Catalog với Cache Eviction Event.
3. Multi-channel Notification (SMS via Twilio / Push Notification via Firebase).
4. Prometheus & Grafana Monitoring Dashboard tự động cảnh báo qua Telegram.

---

## 6. Requirement Traceability Matrix (RTM)

| Requirement ID | Mô tả Yêu cầu | Business Component | Service Phụ Trách | Method / Protocol | Verification Test |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **REQ-01** | User Reg & Auth | Identity Management | Identity Service | REST / JWT | JUnit 5 / Postman |
| **REQ-02** | Gateway Auth Filter | Security Gateway | API Gateway | Reactive Filter | Integration Test |
| **REQ-03** | Product & SKU CRUD | Catalog Management | Product Service | REST / MongoDB | Postman / JUnit 5 |
| **REQ-04** | Fast Product Search | Search Engine | Product Service | REST / Elasticsearch | k6 Search Test |
| **REQ-05** | Internal SKU Validation| Product Inspection | Order & Product | gRPC (HTTP/2) | gRPC Integration Test|
| **REQ-06** | Atomic Stock Reserve | Inventory Control | Order Service | REST / PostgreSQL | k6 Concurrency Test |
| **REQ-07** | Checkout Saga Flow | Distributed Tx | Order, Payment, Noti | AMQP (RabbitMQ) | Saga State Test |
| **REQ-08** | Transactional Outbox | Event Reliability | Order & Payment | PostgreSQL + Outbox | Testcontainers Test |
| **REQ-09** | Order Email Notification| Async Communication | Notification Service| RabbitMQ Consumer | Email Mock Test |
| **REQ-10** | Continuous Deployment | DevOps Infrastructure | CI/CD & Docker | GitHub Actions / GHCR| Deployment Test |
