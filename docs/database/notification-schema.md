# Notification Service Database Schema Specification (MongoDB)

## 1. Overview
Database `notification_db` sử dụng **MongoDB 7.0** để lưu trữ nhật ký thông báo (Notification Logs), Mẫu thông báo (Templates) và danh sách sự kiện đã xử lý để đảm bảo Idempotent Consumer.

---

## 2. MongoDB Document Schemas

### 2.1. Notification Logs Collection (`notification_logs`)
```json
{
  "_id": { "$oid": "651f1a2b3c4d5e6f7a8b9c10" },
  "recipientEmail": "user@example.com",
  "notificationType": "ORDER_CONFIRMATION",
  "subject": "Xác nhận đơn hàng #ORD-8888",
  "content": "<html><body>Cảm ơn bạn đã mua hàng...</body></html>",
  "status": "SENT",
  "errorMessage": null,
  "retryCount": 0,
  "eventId": "evt-uuid-1001",
  "createdAt": { "$date": "2026-10-08T10:05:00Z" }
}
```

### 2.2. Notification Templates Collection (`notification_templates`)
```json
{
  "_id": { "$oid": "651f1a2b3c4d5e6f7a8b9c11" },
  "templateCode": "EMAIL_ORDER_CONFIRMATION",
  "subject": "Xác nhận đơn hàng #{{orderNumber}}",
  "bodyTemplate": "Cháo {{customerName}}, đơn hàng {{orderNumber}} trị giá {{totalAmount}} đã được xác nhận.",
  "isActive": true
}
```

### 2.3. Processed Events Collection (`processed_events`)
```json
{
  "_id": "evt-uuid-1001",
  "eventType": "OrderConfirmedEvent",
  "processedAt": { "$date": "2026-10-08T10:05:01Z" }
}
```

### Indexes:
- `db.notification_logs.createIndex({ "recipientEmail": 1, "createdAt": -1 })`
- `db.notification_logs.createIndex({ "eventId": 1 })`
- `db.notification_templates.createIndex({ "templateCode": 1 }, { unique: true })`
