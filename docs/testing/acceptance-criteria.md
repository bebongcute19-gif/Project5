# Acceptance Criteria (AC) — Omnichannel E-Commerce Platform (Project MD5)

## 1. API Gateway & Security Context

### AC-GW-01: API Routing & Rate Limiting
- **Given** Khách hàng gửi HTTP request đến API Gateway ở port 8080.
- **When** Request chứa đường dẫn `/api/v1/products/123`.
- **Then** API Gateway phải chuyển tiếp request đến `Product Catalog Service` (Port 8082).
- **And** Nếu khách hàng gửi quá 100 requests/phút từ cùng một IP address, API Gateway trả về HTTP `429 Too Many Requests` với message `"Rate limit exceeded. Try again later."`.

### AC-GW-02: JWT Authentication Verification
- **Given** Request gọi API yêu cầu xác thực (ví dụ `POST /api/v1/orders`).
- **When** Request chứa header `Authorization: Bearer <valid_jwt_token>`.
- **Then** API Gateway phải verify thành công signature của JWT, trích xuất `userId` và `roles`, inject vào headers `X-User-Id` và `X-User-Roles` gửi đến Order Service.
- **When** Header `Authorization` bị thiếu hoặc JWT hết hạn / sai chữ ký.
- **Then** API Gateway lập tức ngắt request và trả về HTTP `401 Unauthorized` dạng RFC 7807 Problem Details.

---

## 2. Identity Service Context

### AC-ID-01: User Registration
- **Given** Dữ liệu đăng ký gồm `email`, `password` (tối thiểu 8 ký tự, có chữ hoa, chữ số), `fullName`, `phone`.
- **When** Khách hàng gọi `POST /api/v1/auth/register`.
- **Then** Mật khẩu phải được mã hóa BCrypt trước khi lưu DB.
- **And** Trả về HTTP `201 Created` kèm thông tin User (không bao gồm password).
- **And** Nếu Email đã tồn tại trong DB, trả về HTTP `409 Conflict`.

### AC-ID-02: User Login & Token Refresh
- **Given** Tài khoản đã được đăng ký hợp lệ.
- **When** Khách hàng gọi `POST /api/v1/auth/login` với email và password đúng.
- **Then** Trả về HTTP `200 OK` kèm `accessToken` (TTL 15m) và `refreshToken` (TTL 7 ngày).
- **When** Khách hàng gọi `POST /api/v1/auth/refresh-token` với `refreshToken` hợp lệ.
- **Then** Trả về cặp `accessToken` mới và `refreshToken` mới (Token Rotation).

---

## 3. Product Catalog Context

### AC-PR-01: Product & SKU Creation (Admin)
- **Given** Quản trị viên đã đăng nhập với role `ROLE_ADMIN`.
- **When** Admin gọi `POST /api/v1/products` với payload sản phẩm có 2 biến thể SKUs (Size M, Size L).
- **Then** Lưu thông tin sản phẩm và SKUs vào MongoDB.
- **And** Phát tín hiệu Sync hoặc tự động index thông tin sản phẩm vào Elasticsearch.
- **And** Trả về HTTP `201 Created` với `productId` và danh sách `skuCode`.

### AC-PR-02: Product Search via Elasticsearch
- **Given** Elasticsearch đã được index danh mục sản phẩm.
- **When** Khách hàng gọi `GET /api/v1/products/search?keyword=iPhone&category=electronics&page=0&size=10`.
- **Then** Trả về kết quả tìm kiếm full-text trong thời gian < 100ms.
- **And** Payload trả về có đầy đủ thông tin phân trang: `content`, `pageNumber`, `pageSize`, `totalElements`, `totalPages`.

### AC-PR-03: gRPC Inter-service SKU Verification
- **Given** Order Service cần kiểm tra 2 SKUs `SKU-IPHONE15-BLK` và `SKU-CASE-CLR`.
- **When** Order Service gọi gRPC method `GetProductVariants(VariantRequest)` qua HTTP/2.
- **Then** Product Service phải trả về thông tin chi tiết (tên, giá hiện tại, trạng thái active) của các SKUs trong vòng < 5ms.

---

## 4. Order & Inventory Context (Core Business)

### AC-OR-01: Order Creation & Atomic Stock Reservation
- **Given** SKU `SKU-MACBOOK-M3` có `availableQuantity = 1` và `reservedQuantity = 0`.
- **When** Khách hàng A gọi `POST /api/v1/orders` với `quantity = 1` kèm `Idempotency-Key: idempotency-uuid-101`.
- **Then** Hệ thống thực thi câu lệnh SQL Atomic Conditional Update:
  `UPDATE inventory SET available_quantity = available_quantity - 1, reserved_quantity = reserved_quantity + 1 WHERE sku_code = 'SKU-MACBOOK-M3' AND available_quantity >= 1;`
- **And** Cập nhật kho thành công, tạo đơn hàng trạng thái `PENDING_PAYMENT`, lưu `OutboxEvent` (`OrderCreatedEvent`), trả về HTTP `201 Created`.
- **And** Tạo bản ghi Stock Reservation có TTL = 15 phút.

### AC-OR-02: Flash Sale Concurrency Test (Zero Oversell Guarantee)
- **Given** SKU `SKU-FLASH-ITEM` còn duy nhất 1 sản phẩm trong kho.
- **When** 100 người dùng gửi 100 request đồng thời tranh mua SKU này (`POST /api/v1/orders`).
- **Then** Chỉ đúng 1 request được xử lý thành công (HTTP `201 Created`), 99 requests còn lại nhận lỗi HTTP `400 Bad Request` hoặc `409 Conflict` với thông báo `"Sản phẩm đã hết hàng"`.
- **And** Giá trị `availableQuantity` bằng 0, không bao giờ bị âm (`availableQuantity >= 0`).

### AC-OR-03: Idempotency Verification
- **Given** Khách hàng A gọi `POST /api/v1/orders` với header `Idempotency-Key: unique-key-999`.
- **When** Do chập chờn mạng, Khách hàng A gửi lại đúng request đó 3 lần liên tiếp.
- **Then** Chỉ đúng 1 đơn hàng duy nhất được tạo trong DB và kho chỉ bị trừ 1 lần.
- **And** 2 requests sau nhận lại chính xác HTTP Response của request đầu tiên từ Redis/DB Idempotency cache.

---

## 5. Payment Context & Saga Compensation

### AC-PA-01: Payment Simulation & Event Publishing
- **Given** Đơn hàng `ORD-8888` đang ở trạng thái `PENDING_PAYMENT` với số tiền 1,000,000 VND.
- **When** Order Service phát event `OrderCreatedEvent` hoặc Client gọi `POST /api/v1/payments/process`.
- **Then** Payment Service tạo bản ghi `PaymentTransaction` trạng thái `SUCCESS`.
- **And** Lưu `OutboxEvent` (`PaymentSucceededEvent`) trong cùng một Local Database Transaction.
- **And** Outbox Publisher đẩy event lên RabbitMQ `payment.exchange` với routing key `payment.succeeded`.

### AC-PA-02: Payment Failure Compensation Saga
- **Given** Đơn hàng `ORD-9999` có `reservedQuantity = 1`.
- **When** Khách hàng thanh toán thất bại (Tài khoản không đủ tiền hoặc timeout).
- **Then** Payment Service phát event `PaymentFailedEvent`.
- **And** Order Service nhận `PaymentFailedEvent`, kích hoạt Compensation Transaction:
  1. Hoàn trả kho: `UPDATE inventory SET available_quantity = available_quantity + 1, reserved_quantity = reserved_quantity - 1 WHERE sku_code = '...';`
  2. Cập nhật trạng thái đơn hàng sang `CANCELLED`.
- **And** Notification Service gửi mail thông báo đơn hàng đã bị hủy do thanh toán không thành công.

---

## 6. Notification Context & Reliable Messaging

### AC-NO-01: Asynchronous Order Confirmation Email
- **Given** Event `OrderConfirmedEvent` hoặc `PaymentSucceededEvent` được đẩy vào RabbitMQ queue `notification.order.confirmed.queue`.
- **When** Notification Service consume được message.
- **Then** Trích xuất thông tin khách hàng, render mẫu HTML email xác nhận đơn hàng và gửi qua SMTP Server.
- **And** Ghi bản ghi `notification_logs` với status `SENT` và `messageId`.

### AC-NO-02: Idempotent Consumer & DLQ Retry
- **Given** RabbitMQ gửi lại trùng lặp 1 message (Duplicate Event Delivery).
- **When** Notification Service kiểm tra bảng `processed_events` thấy `eventId` đã được xử lý.
- **Then** Bỏ qua không gửi mail lần 2, thực hiện ACK message tới RabbitMQ.
- **When** Việc gửi email bị lỗi 3 lần liên tiếp do SMTP Server ngắt kết nối.
- **Then** Message được chuyển tới Dead Letter Queue (`notification.dlq`) để quản trị viên kiểm tra và retry thủ công.
