# Testing Strategy & Quality Assurance — Omnichannel E-Commerce Platform (Project MD5)

## 1. Mức độ & Tầng Kiểm Thử (Testing Pyramid)

Hệ thống được thiết kế theo chiến lược kiểm thử đa tầng để đảm bảo độ tin cậy chuẩn Enterprise:

```
        / \
       /   \     E2E & Load Testing (k6)
      /  k6 \    - Flash Sale Stress Test, Concurrency, Latency P95
     /-------\
    /         \   Integration Testing (Testcontainers & SpringBootTest)
   /   ITs     \  - Database ACID, Real PostgreSQL/MongoDB, RabbitMQ Event Flow
  /-------------\
 /               \ Unit Testing (JUnit 5 & Mockito)
/     Unit        \ - Domain Logic, Aggregates, State Machine, Validations
-------------------
```

---

## 2. Unit Testing Strategy (JUnit 5 + Mockito)

- **Phạm vi:** Kiểm thử độc lập các Domain Entities, Value Objects, Aggregates, Domain Services, DTO Mapping và Policy Rules.
- **Công cụ:** JUnit 5 (Jupiter), AssertJ, Mockito.
- **Mục tiêu Coverage:** Code Coverage tối thiểu **80%** trên toàn bộ Business Logic.

### Ví dụ Unit Test Suite:
1. `OrderAggregateTest`: Kiểm tra trạng thái chuyển đổi của Order (`canBeCancelled()`, `reserveStock()`).
2. `InventoryUnitTest`: Kiểm tra tính toán số lượng tồn kho khả dụng (`availableQuantity = totalQuantity - reservedQuantity`).
3. `JwtTokenProviderTest`: Kiểm tra việc sinh token, trích xuất Claims và kiểm tra token hết hạn.

---

## 3. Integration Testing Strategy (Testcontainers)

Để đảm bảo kiểm thử chính xác tuyệt đối mà không cần dùng In-Memory DB (như H2) dễ gây sai lệch SQL Dialect, toàn bộ Integration Tests sử dụng **Testcontainers** để khởi tạo container thật trong khi chạy test.

- **Môi trường Testcontainers:**
  - **PostgreSQL Container (`postgres:16-alpine`):** Dùng để test PostgreSQL Atomic Updates, Transactions, Indexing.
  - **MongoDB Container (`mongo:7.0`):** Dùng để test Mongo Repository, Product Documents.
  - **RabbitMQ Container (`rabbitmq:3.12-management-alpine`):** Dùng để test Publisher Confirms, Consumer Manual ACK, Dead Letter Queue.
  - **Redis Container (`redis:7.2-alpine`):** Dùng để test Distributed Lock & Rate Limiting.

### Các Kịch bản Integration Test Trọng Tâm:
1. **`OrderInventoryRepositoryIT`:** Test trực tiếp câu lệnh Atomic Conditional Update `UPDATE inventory SET available_quantity = available_quantity - :qty WHERE ...` trên container PostgreSQL thật dưới môi trường đa luồng (Multi-threading with ExecutorService).
2. **`OutboxRelaySchedulerIT`:** Test việc lưu bản ghi vào `outbox_events` -> Polling Scheduler -> Publish lên RabbitMQ Container -> Đánh dấu `processed = true`.
3. **`IdempotentConsumerIT`:** Test việc gửi cùng một `eventId` 2 lần tới Consumer, xác minh bảng `processed_events` ngăn chặn xử lý trùng lặp.

---

## 4. Performance & Concurrency Stress Testing Strategy (k6)

Công cụ **k6** (Grafana k6) được sử dụng làm chuẩn kiểm thử hiệu năng và khả năng chịu tải concurrency trong các đợt Flash Sale.

### Kịch bản Test 1: Flash Sale Concurrency Test (`flash_sale_concurrency.js`)
- **Mục tiêu:** 100 virtual users (VUs) gửi đồng thời 100 POST request tạo đơn hàng trong khoảng 1 giây cho 1 sản phẩm chỉ còn 1 tồn kho.
- **Tiêu chí Đạt (Pass Criteria):**
  - Số đơn hàng tạo thành công (`HTTP 201`): Đúng 1 request.
  - Số đơn hàng nhận thông báo hết hàng (`HTTP 400` / `409`): 99 requests.
  - Tồn kho khả dụng cuối cùng trong DB: Đúng `0`.
  - Tồn kho âm: **0 (Tuyệt đối không bị âm)**.

#### Sample k6 Script Template:
```javascript
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    flash_sale_spike: {
      executor: 'per-vu-iterations',
      vus: 100,
      iterations: 1,
      maxDuration: '10s',
    },
  },
};

export default function () {
  const url = 'http://localhost:8080/api/v1/orders';
  const payload = JSON.stringify({
    items: [{ skuCode: 'SKU-FLASH-001', quantity: 1 }],
    shippingAddressId: 'addr-001'
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + __ENV.JWT_TOKEN,
      'Idempotency-Key': `k6-req-${__VU}-${Date.now()}`
    },
  };

  const res = http.post(url, payload, params);

  check(res, {
    'Status is 201 or 409/400': (r) => r.status === 201 || r.status === 409 || r.status === 400,
  });
}
```

### Kịch bản Test 2: Search & Read API Load Test (`product_search_load.js`)
- **Mục tiêu:** Duy trì 1,000 Virtual Users trong 5 phút gọi API tìm kiếm sản phẩm qua Elasticsearch.
- **Tiêu chí Đạt (Pass Criteria):**
  - Latency P95 < 100ms.
  - Rate of errors (HTTP 5xx) < 0.1%.

---

## 5. End-to-End (E2E) Saga Verification

- Kiểm thử toàn bộ luồng từ Client -> API Gateway -> Order Service -> Product Service (gRPC) -> Payment Service Simulation -> RabbitMQ -> Notification Service -> Order Confirmation Mail.
- Giả lập lỗi mạng hoặc lỗi Payment để verify Compensation Transaction giải phóng tồn kho thành công.
