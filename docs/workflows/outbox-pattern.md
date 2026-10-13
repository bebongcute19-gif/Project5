# Transactional Outbox & Idempotent Consumer Specification — Project MD5

## 1. Vấn đề Dual-Write trong Kiến trúc Microservices

Khi một Microservice cần vừa ghi dữ liệu vào Database của mình, vừa phát sự kiện (Publish Event) lên Message Broker (RabbitMQ), hành động này tạo ra sự cố **Dual-Write Problem**:
- Nếu DB commit thành công nhưng RabbitMQ bị ngắt kết nối -> Event bị mất vĩnh viễn, dữ liệu giữa các service bị bất đồng bộ.
- Nếu gửi RabbitMQ trước nhưng DB rollback -> Consumer xử lý sự kiện cho một dữ liệu không tồn tại.

---

## 2. Giải pháp Transactional Outbox Pattern

```mermaid
sequenceDiagram
    autonumber
    participant App as Order Application
    participant DB as PostgreSQL Database
    participant Poller as Outbox Relay Scheduler
    participant MQ as RabbitMQ Broker

    rect rgb(240, 248, 255)
        note over App,DB: Local Database Transaction (Atomic)
        App->>DB: 1. INSERT INTO orders ...
        App->>DB: 2. UPDATE inventory ...
        App->>DB: 3. INSERT INTO outbox_events (event_type, payload, processed=false)
        DB-->>App: Commit Transaction Success!
    end

    rect rgb(240, 255, 240)
        note over Poller,MQ: Async Outbox Relay Loop (Fixed Rate 500ms)
        Poller->>DB: SELECT * FROM outbox_events WHERE processed = false LIMIT 50
        DB-->>Poller: Return unprocessed events
        loop For each event
            Poller->>MQ: Publish Event to Exchange with Publisher Confirms
            MQ-->>Poller: ACK Received from RabbitMQ
            Poller->>DB: UPDATE outbox_events SET processed = true WHERE id = :id
        end
    end
```

### Chi tiết Bảng `outbox_events`:
```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL, -- JSON String
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_unprocessed ON outbox_events(processed, created_at) WHERE processed = FALSE;
```

---

## 3. Idempotent Consumer Pattern (Chống trùng lặp Message)

Do RabbitMQ đảm bảo cơ chế **At-Least-Once Delivery**, sự kiện có thể được gửi lặp lại nhiều hơn 1 lần (ví dụ do đứt mạng đúng lúc Poller đang chờ ACK). Consumer bắt buộc phải áp dụng **Idempotent Consumer Pattern**.

```mermaid
flowchart TD
    MQMessage["Message Received from RabbitMQ"] --> ExtractEventId["Extract eventId & eventType"]
    ExtractEventId --> DBCheck{"Check processed_events table<br>EXISTS(eventId)?"}
    
    DBCheck -->|Yes - Duplicate!| ACK_Skip["1. ACK Message to RabbitMQ<br>2. Log Warning & Skip Execution"]
    DBCheck -->|No - New Event| ExecuteBusiness["1. Execute Business Logic<br>2. INSERT INTO processed_events(eventId)<br>3. Commit Local Transaction"]
    
    ExecuteBusiness -->|Success| ACK_Success["ACK Message to RabbitMQ"]
    ExecuteBusiness -->|Exception Fail| NACK_Retry["NACK / Reject Message<br>(Re-queue or send to DLQ)"]
```

### Chi tiết Bảng `processed_events`:
```sql
CREATE TABLE processed_events (
    event_id VARCHAR(128) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```
