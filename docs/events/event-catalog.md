# Domain Event Catalog Specification — Project MD5

## 1. Metadata Standard của Domain Events
Toàn bộ các sự kiện bất đồng bộ trong hệ thống được đóng gói theo cấu trúc Metadata chuẩn Enterprise:

```json
{
  "eventId": "evt-uuid-1000-2000-3000",
  "eventType": "OrderCreatedEvent",
  "aggregateType": "Order",
  "aggregateId": "ord-uuid-8888",
  "timestamp": "2026-10-08T10:30:00Z",
  "version": "1.0",
  "payload": { ... }
}
```

---

## 2. Catalog Chi tiết Các Domain Events

### 2.1. `OrderCreatedEvent`
- **Publisher:** `Order & Inventory Service`
- **Exchange:** `order.exchange`
- **Routing Key:** `order.created`
- **Description:** Phát ra ngay sau khi đơn hàng được tạo và tồn kho được tạm giữ thành công trong DB local transaction.
- **Payload Schema:**
```json
{
  "eventId": "evt-001",
  "eventType": "OrderCreatedEvent",
  "aggregateId": "ord-8888",
  "timestamp": "2026-10-08T10:30:00Z",
  "payload": {
    "orderId": "ord-8888",
    "orderNumber": "ORD-20261008-001",
    "customerId": "usr-101",
    "totalAmount": 1500000.00,
    "items": [
      { "skuCode": "SKU-NIKE-BLK-M", "quantity": 1, "price": 1500000.00 }
    ],
    "shippingAddress": {
      "recipientName": "Nguyen Van A",
      "phone": "0901234567",
      "streetAddress": "123 Le Loi",
      "city": "Ho Chi Minh"
    }
  }
}
```

---

### 2.2. `PaymentSucceededEvent`
- **Publisher:** `Payment Service`
- **Exchange:** `payment.exchange`
- **Routing Key:** `payment.succeeded`
- **Description:** Phát ra khi giao dịch thanh toán thành công.
- **Payload Schema:**
```json
{
  "eventId": "evt-002",
  "eventType": "PaymentSucceededEvent",
  "aggregateId": "pay-9999",
  "timestamp": "2026-10-08T10:31:00Z",
  "payload": {
    "paymentId": "pay-9999",
    "orderId": "ord-8888",
    "transactionRef": "TXN-VNPAY-777",
    "amount": 1500000.00,
    "paymentMethod": "VNPAY"
  }
}
```

---

### 2.3. `PaymentFailedEvent`
- **Publisher:** `Payment Service`
- **Exchange:** `payment.exchange`
- **Routing Key:** `payment.failed`
- **Description:** Phát ra khi giao dịch thanh toán bị từ chối hoặc thất bại.
- **Payload Schema:**
```json
{
  "eventId": "evt-003",
  "eventType": "PaymentFailedEvent",
  "aggregateId": "pay-9999",
  "timestamp": "2026-10-08T10:31:00Z",
  "payload": {
    "paymentId": "pay-9999",
    "orderId": "ord-8888",
    "errorCode": "INSUFFICIENT_FUNDS",
    "errorMessage": "Tài khoản không đủ số dư"
  }
}
```

---

### 2.4. `OrderConfirmedEvent`
- **Publisher:** `Order & Inventory Service`
- **Exchange:** `order.exchange`
- **Routing Key:** `order.confirmed`
- **Description:** Phát ra khi Order Service nhận được `PaymentSucceededEvent` và chuyển trạng thái đơn hàng sang `CONFIRMED`.
- **Payload Schema:**
```json
{
  "eventId": "evt-004",
  "eventType": "OrderConfirmedEvent",
  "aggregateId": "ord-8888",
  "timestamp": "2026-10-08T10:31:05Z",
  "payload": {
    "orderId": "ord-8888",
    "orderNumber": "ORD-20261008-001",
    "customerId": "usr-101",
    "confirmedAt": "2026-10-08T10:31:05Z"
  }
}
```

---

### 2.5. `OrderCancelledEvent`
- **Publisher:** `Order & Inventory Service`
- **Exchange:** `order.exchange`
- **Routing Key:** `order.cancelled`
- **Description:** Phát ra khi đơn hàng bị hủy do thanh toán thất bại hoặc hết hạn tạm giữ kho.
- **Payload Schema:**
```json
{
  "eventId": "evt-005",
  "eventType": "OrderCancelledEvent",
  "aggregateId": "ord-8888",
  "timestamp": "2026-10-08T10:32:00Z",
  "payload": {
    "orderId": "ord-8888",
    "orderNumber": "ORD-20261008-001",
    "reason": "Payment failed: INSUFFICIENT_FUNDS"
  }
}
```
