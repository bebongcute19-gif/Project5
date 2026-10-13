package com.omnichannel.product.controller;

import com.omnichannel.product.dto.request.CreateProductVariantRequest;
import com.omnichannel.product.dto.request.UpdateProductVariantRequest;
import com.omnichannel.product.dto.response.ProductVariantResponse;
import com.omnichannel.product.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/{productId}/variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    @PostMapping
    public ResponseEntity<ProductVariantResponse> createVariant(
            @PathVariable String productId,
            @Valid @RequestBody CreateProductVariantRequest request) {
        ProductVariantResponse response = productVariantService.createVariant(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductVariantResponse>> listVariants(@PathVariable String productId) {
        List<ProductVariantResponse> response = productVariantService.listVariants(productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<ProductVariantResponse> getVariant(
            @PathVariable String productId,
            @PathVariable String variantId) {
        ProductVariantResponse response = productVariantService.getVariantById(productId, variantId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{variantId}")
    public ResponseEntity<ProductVariantResponse> updateVariant(
            @PathVariable String productId,
            @PathVariable String variantId,
            @Valid @RequestBody UpdateProductVariantRequest request) {
        ProductVariantResponse response = productVariantService.updateVariant(productId, variantId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<Void> deleteVariant(
            @PathVariable String productId,
            @PathVariable String variantId) {
        productVariantService.deleteVariant(productId, variantId);
        return ResponseEntity.noContent().build();
    }
}
