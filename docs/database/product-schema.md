# Product Catalog Service Database & Elasticsearch Schema Specification

## 1. Overview
Database `product_db` sử dụng **MongoDB 7.0** làm primary storage lưu document linh hoạt cho Sản phẩm, Biến thể SKU, Danh mục và Thương hiệu. Kết hợp **Elasticsearch 8.11** làm Search Engine để phục vụ tìm kiếm full-text và lọc sản phẩm tốc độ cao.

---

## 2. MongoDB Document Schemas

### 2.1. Categories Collection (`categories`)
```json
{
  "_id": { "$oid": "651f1a2b3c4d5e6f7a8b9c01" },
  "name": "Thời trang Nam",
  "slug": "thoi-trang-nam",
  "description": "Danh mục thời trang dành cho nam giới",
  "parentCategoryId": null,
  "isActive": true,
  "createdAt": { "$date": "2026-10-08T10:00:00Z" }
}
```

### 2.2. Brands Collection (`brands`)
```json
{
  "_id": { "$oid": "651f1a2b3c4d5e6f7a8b9c02" },
  "name": "Nike",
  "slug": "nike",
  "logoUrl": "https://cdn.example.com/brands/nike.png",
  "isActive": true,
  "createdAt": { "$date": "2026-10-08T10:00:00Z" }
}
```

### 2.3. Products Collection (`products`)
```json
{
  "_id": { "$oid": "651f1a2b3c4d5e6f7a8b9c03" },
  "name": "Áo Thun Sportwear Nike",
  "slug": "ao-thun-sportwear-nike",
  "description": "Áo thun thể thao thoáng khí",
  "categoryId": "651f1a2b3c4d5e6f7a8b9c01",
  "brandId": "651f1a2b3c4d5e6f7a8b9c02",
  "status": "ACTIVE",
  "images": [
    "https://cdn.example.com/products/shirt-1.jpg"
  ],
  "attributes": [
    { "key": "Material", "value": "100% Cotton" },
    { "key": "Gender", "value": "Men" }
  ],
  "variants": [
    {
      "skuCode": "SKU-NIKE-TSHIRT-BLK-M",
      "variantName": "Đen / Size M",
      "regularPrice": 500000,
      "salePrice": 450000,
      "attributes": {
        "color": "Black",
        "size": "M"
      },
      "isActive": true
    },
    {
      "skuCode": "SKU-NIKE-TSHIRT-BLK-L",
      "variantName": "Đen / Size L",
      "regularPrice": 500000,
      "salePrice": 450000,
      "attributes": {
        "color": "Black",
        "size": "L"
      },
      "isActive": true
    }
  ],
  "createdAt": { "$date": "2026-10-08T10:00:00Z" },
  "updatedAt": { "$date": "2026-10-08T10:00:00Z" }
}
```

### MongoDB Indexes:
- `db.products.createIndex({ "variants.skuCode": 1 }, { unique: true })`
- `db.products.createIndex({ "categoryId": 1, "status": 1 })`
- `db.products.createIndex({ "slug": 1 }, { unique: true })`

---

## 3. Elasticsearch Index Mapping (`products_index`)

```json
{
  "mappings": {
    "properties": {
      "productId": { "type": "keyword" },
      "name": { 
        "type": "text",
        "analyzer": "standard",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "slug": { "type": "keyword" },
      "description": { "type": "text" },
      "categoryId": { "type": "keyword" },
      "categoryName": { "type": "text" },
      "brandId": { "type": "keyword" },
      "brandName": { "type": "text" },
      "status": { "type": "keyword" },
      "minPrice": { "type": "double" },
      "maxPrice": { "type": "double" },
      "skus": {
        "type": "nested",
        "properties": {
          "skuCode": { "type": "keyword" },
          "price": { "type": "double" },
          "attributes": { "type": "object" }
        }
      },
      "createdAt": { "type": "date" }
    }
  }
}
```
