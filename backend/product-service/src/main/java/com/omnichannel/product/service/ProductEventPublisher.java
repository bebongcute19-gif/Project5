package com.omnichannel.product.service;

import com.omnichannel.product.entity.ProductEvent;
import com.omnichannel.product.entity.ProductVariant;

import java.util.UUID;

public interface ProductEventPublisher {

    /**
     * Publish a PRODUCT_CREATED event to outbox
     */
    void publishProductCreated(UUID productId);

    /**
     * Publish a PRODUCT_UPDATED event to outbox
     */
    void publishProductUpdated(UUID productId);

    /**
     * Publish a PRODUCT_STATUS_CHANGED event to outbox
     */
    void publishProductStatusChanged(UUID productId, String oldStatus, String newStatus);

    /**
     * Publish a PRODUCT_DELETED event to outbox (soft delete)
     */
    void publishProductDeleted(UUID productId);

    /**
     * Publish a VARIANT_PRICE_CHANGED event to outbox
     */
    void publishVariantPriceChanged(ProductVariant variant);
}
