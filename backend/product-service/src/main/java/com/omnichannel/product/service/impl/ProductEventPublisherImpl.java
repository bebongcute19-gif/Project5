package com.omnichannel.product.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichannel.product.entity.Product;
import com.omnichannel.product.entity.ProductEvent;
import com.omnichannel.product.entity.ProductVariant;
import com.omnichannel.product.repository.ProductEventRepository;
import com.omnichannel.product.repository.ProductRepository;
import com.omnichannel.product.service.ProductEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductEventPublisherImpl implements ProductEventPublisher {

    private final ProductEventRepository eventRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publishProductCreated(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", product.getId().toString());
        payload.put("name", product.getName());
        payload.put("slug", product.getSlug());
        payload.put("status", product.getStatus());
        payload.put("categoryId", product.getCategoryId().toString());
        payload.put("brandId", product.getBrandId().toString());

        createEvent(productId, ProductEvent.EventType.PRODUCT_CREATED, payload);
        log.info("Published PRODUCT_CREATED event for product: {}", productId);
    }

    @Override
    public void publishProductUpdated(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", product.getId().toString());
        payload.put("name", product.getName());
        payload.put("description", product.getDescription());
        payload.put("slug", product.getSlug());
        payload.put("status", product.getStatus());
        payload.put("categoryId", product.getCategoryId().toString());
        payload.put("brandId", product.getBrandId().toString());

        createEvent(productId, ProductEvent.EventType.PRODUCT_UPDATED, payload);
        log.info("Published PRODUCT_UPDATED event for product: {}", productId);
    }

    @Override
    public void publishProductStatusChanged(UUID productId, String oldStatus, String newStatus) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId.toString());
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);

        createEvent(productId, ProductEvent.EventType.PRODUCT_STATUS_CHANGED, payload);
        log.info("Published PRODUCT_STATUS_CHANGED event for product: {} (from {} to {})", productId, oldStatus, newStatus);
    }

    @Override
    public void publishProductDeleted(UUID productId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId.toString());

        createEvent(productId, ProductEvent.EventType.PRODUCT_DELETED, payload);
        log.info("Published PRODUCT_DELETED event for product: {}", productId);
    }

    @Override
    public void publishVariantPriceChanged(ProductVariant variant) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("variantId", variant.getId().toString());
        payload.put("productId", variant.getProductId().toString());
        payload.put("skuCode", variant.getSkuCode());
        payload.put("regularPrice", variant.getRegularPrice().toString());
        payload.put("salePrice", variant.getSalePrice() != null ? variant.getSalePrice().toString() : null);

        createEvent(variant.getProductId(), ProductEvent.EventType.VARIANT_PRICE_CHANGED, payload);
        log.info("Published VARIANT_PRICE_CHANGED event for variant: {}", variant.getId());
    }

    private void createEvent(UUID productId, ProductEvent.EventType eventType, Map<String, Object> payload) {
        try {
            ProductEvent event = ProductEvent.builder()
                    .productId(productId)
                    .eventType(eventType)
                    .eventTimestamp(Instant.now())
                    .eventPayload(objectMapper.valueToTree(payload))
                    .processed(false)
                    .retryCount(0)
                    .maxRetries(3)
                    .build();

            eventRepository.save(event);
            log.debug("Event saved to outbox: type={}, productId={}", eventType, productId);
        } catch (Exception e) {
            log.error("Error creating event in outbox: type={}, productId={}", eventType, productId, e);
            // Don't throw - we want Product operations to succeed even if event publishing fails
            // The events will be retried later
        }
    }
}
