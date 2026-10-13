package com.omnichannel.product.controller;

import com.omnichannel.product.dto.request.CreateProductRequest;
import com.omnichannel.product.dto.request.UpdateProductRequest;
import com.omnichannel.product.dto.response.ProductResponse;
import com.omnichannel.product.service.ProductSearchService;
import com.omnichannel.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductSearchService productSearchService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductResponse> getProductBySlug(@PathVariable String slug) {
        ProductResponse response = productService.getProductBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> listProducts(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<ProductResponse> response = productService.listProducts(categoryId, brandId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ProductResponse> getProductByIdPublic(@PathVariable String id) {
        ProductResponse response = productService.getProductByIdPublic(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/list")
    public ResponseEntity<Page<ProductResponse>> listProductsPublic(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brandId,
            Pageable pageable) {
        Page<ProductResponse> response = productService.listProductsPublic(categoryId, brandId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            Pageable pageable) {
        Page<ProductResponse> response = productSearchService.searchProducts(
                keyword, categoryId, brandId, minPrice, maxPrice, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search/reindex")
    public ResponseEntity<Void> reindexAllProducts() {
        productSearchService.reindexAllProducts();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/search/reindex-batch")
    public ResponseEntity<Void> reindexAllProductsWithBatching(
            @RequestParam(defaultValue = "100") int batchSize) {
        productSearchService.reindexAllProductsWithBatching(batchSize);
        return ResponseEntity.accepted().build();
    }
}
