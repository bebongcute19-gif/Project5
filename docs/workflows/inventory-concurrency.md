# High-Concurrency Inventory Control Specification (Task 7) — Project MD5

## 1. Bài toán Flash Sale & Race Condition Benchmark

### Kịch bản
Trong một đợt Flash Sale, sản phẩm SKU `SKU-IPHONE15-PRO-1TB` chỉ còn duy nhất **1 sản phẩm** trong kho (`available_quantity = 1`). Đúng 12:00:00, có **100 yêu cầu mua hàng (Concurrent Requests)** đồng thời đổ vào API `POST /api/v1/orders`.

### Yêu cầu Bắt buộc
1. **Zero Overselling:** Chỉ duy nhất 1 request mua thành công.
2. **No Negative Inventory:** Số lượng `available_quantity` không bao giờ bị âm (`>= 0`).
3. **No Over-reservation:** Không bao giờ tạo thành công nhiều hơn 1 bản ghi reservation.
4. **Idempotency with Retries:** Khi client hoặc hệ thống retry request, kho không bao giờ bị trừ hai lần.
5. **SSOT Guarantee:** Database chính (PostgreSQL) phải là Single Source of Truth (SSOT). Redis chỉ đóng vai trò L2 Cache / Rate Limiter, không dùng làm SSOT để tránh rủi ro đứt gãy dữ liệu (Memory Loss / Async Replication Lag).

---

## 2. Phân tích Các Phương Pháp Xử lý Concurrency trên PostgreSQL

### 2.1. Phương pháp 1: Pessimistic Locking (`SELECT FOR UPDATE`)
```sql
BEGIN;
-- Khóa dòng dữ liệu inventory của SKU
SELECT id, available_quantity, reserved_quantity 
FROM inventory 
WHERE sku_code = 'SKU-IPHONE15-PRO-1TB' 
FOR UPDATE;

-- Kiểm tra logic trên Java App
-- If available_quantity >= req_qty THEN:
UPDATE inventory 
SET available_quantity = available_quantity - 1, 
    reserved_quantity = reserved_quantity + 1 
WHERE sku_code = 'SKU-IPHONE15-PRO-1TB';

COMMIT;
```
- **Ưu điểm:** An toàn tuyệt đối, các transaction được xếp hàng tuần tự.
- **Nhược điểm:** Gây ngắt nghẽn (DB Connection Pool Bottleneck), làm tăng Latency P99 khi có 100-1000 requests cùng chờ lock 1 dòng.

---

### 2.2. Phương pháp 2: Atomic Conditional Update (Được Chọn Triển Khai Main Approach)
Tận dụng cơ chế Row-Level Write Lock tự động của PostgreSQL khi thực thi câu lệnh `UPDATE` có kèm điều kiện WHERE nguyên tử (`Atomic Condition`).

```sql
UPDATE inventory 
SET available_quantity = available_quantity - :requiredQty, 
    reserved_quantity = reserved_quantity + :requiredQty,
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE sku_code = :skuCode 
  AND available_quantity >= :requiredQty;
```

#### Nguyên lý Hoạt động:
1. PostgreSQL thực thi khoá ghi (Write Lock) dòng dữ liệu nguyên tử khi câu lệnh `UPDATE` chạy.
2. Với 100 requests đồng thời:
   - Request đầu tiên kiểm tra điều kiện `available_quantity >= 1` -> **Thắng (True)**. Dòng dữ liệu được update: `available_quantity = 0`, `reserved_quantity = 1`. Trả về `updated_rows = 1`.
   - 99 requests còn lại chờ khoá thả ra lần lượt thực thi. Lúc này điều kiện `available_quantity >= 1` kiểm tra trên dòng dữ liệu đã update -> **Thua (False)**. Trả về `updated_rows = 0`.
3. Trong Spring Boot Code:
```java
int updatedRows = inventoryRepository.reserveStock(skuCode, quantity);
if (updatedRows == 0) {
    throw new OutOfStockException("Sản phẩm đã hết hàng hoặc không đủ số lượng tồn kho.");
}
```

- **Tại sao tối ưu vượt trội?**
  - **Latency cực thấp:** Không cần giữ connection lâu như `SELECT FOR UPDATE`.
  - **Không thể bị âm kho:** Nhờ Constraint `CHECK (available_quantity >= 0)` ở DB schema kết hợp mệnh đề `WHERE available_quantity >= :requiredQty`.
  - **Đảm bảo ACID:** Nằm trong cùng 1 Local Transaction tạo Order.

---

## 3. Vai trò Chuẩn xác của Redis trong Hệ thống Inventory

```
+-------------------------------------------------------------------------------+
|                                 CLIENT REQUEST                                |
+-------------------------------------------------------------------------------+
                                       |
                                       v
                     +-----------------------------------+
                     | API Gateway (Redis Rate Limiter)  | -> Ngăn DDOS/Spam
                     +-----------------------------------+
                                       |
                                       v
                     +-----------------------------------+
                     |  Redis L2 Cache (Stock Status)    | -> Quick-Check: If Stock == 0
                     +-----------------------------------+    Fast-Fail without hitting DB!
                                       | (If Stock > 0)
                                       v
                     +-----------------------------------+
                     | PostgreSQL (Atomic Update SQL)    | -> SINGLE SOURCE OF TRUTH (SSOT)
                     +-----------------------------------+    Zero-Oversell Guaranteed!
```

1. **Redis KHÔNG làm SSOT:** Không thực hiện trừ kho chính thức trên Redis bằng `DECRBY` để coi đó là con số chuẩn cuối cùng, vì nếu Redis sập hoặc failover, dữ liệu tồn kho ghi vào DB sẽ bị xáo trộn.
2. **Redis làm L2 Stock Cache & Fast-Fail Filter:**
   - Redis lưu Key `stock_status:{sku_code}` = `AVAILABLE` hoặc `OUT_OF_STOCK`.
   - Khi `available_quantity` trong PostgreSQL về `0`, Order Service phát tín hiệu cập nhật Redis key sang `OUT_OF_STOCK`.
   - Các requests đến sau lập tức bị ngắt ngay từ tầng Cache/Gateway (Fast-Fail Response trong 2ms) mà không cần chọc xuống PostgreSQL.

---

## 4. Idempotency & Retries Control Flow

- **Chống trừ kho 2 lần khi Retry:**
  - Client bắt buộc truyền `Idempotency-Key` (UUID).
  - Order Service lưu `Idempotency-Key` trong bảng `orders` với thuộc tính `UNIQUE`.
  - Nếu request trùng lặp đến, DB ngắt bằng `Unique Constraint Violation` -> Spring Boot bắt exception và trả lại ngay thông tin đơn hàng đã tạo trước đó mà không thực thi lại SQL `UPDATE inventory`.
