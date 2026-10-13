package com.omnichannel.product.service.impl;

import com.omnichannel.product.entity.Product;
import com.omnichannel.product.entity.ProductEvent;
import com.omnichannel.product.entity.search.ProductSearchDocument;
import com.omnichannel.product.repository.ProductEventRepository;
import com.omnichannel.product.repository.ProductRepository;
import com.omnichannel.product.repository.search.ProductSearchRepository;
import com.omnichannel.product.service.ProductSearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductSearchSyncServiceImpl implements ProductSearchSyncService {

    private final ProductEventRepository eventRepository;
    private final ProductRepository productRepository;
    private final ProductSearchRepository searchRepository;
    private final ProductSearchServiceImpl searchService;

    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    @Override
    public void processPendingEvents() {
        processPendingEvents(DEFAULT_BATCH_SIZE);
    }

    @Override
    public void processPendingEvents(int batchSize) {
        try {
            Pageable pageable = PageRequest.of(0, batchSize);
            Page<ProductEvent> unprocessedEvents = eventRepository.findUnprocessedEvents(pageable);

            if (unprocessedEvents.isEmpty()) {
                log.debug("No pending events to process");
                return;
            }

            log.info("Processing {} pending events", unprocessedEvents.getNumberOfElements());

            for (ProductEvent event : unprocessedEvents.getContent()) {
                try {
                    processEvent(event);
                    event.markAsProcessed();
                    eventRepository.save(event);
                    log.debug("Event processed successfully: id={}, type={}, productId={}", 
                            event.getId(), event.getEventType(), event.getProductId());
                } catch (Exception e) {
                    handleEventProcessingError(event, e);
                }
            }

            log.info("Completed processing batch of {} events", unprocessedEvents.getNumberOfElements());
        } catch (Exception e) {
            log.error("Error processing pending events", e);
        }
    }

    private void processEvent(ProductEvent event) {
        switch (event.getEventType()) {
            case PRODUCT_CREATED:
                handleProductCreated(event);
                break;
            case PRODUCT_UPDATED:
                handleProductUpdated(event);
                break;
            case PRODUCT_STATUS_CHANGED:
                handleProductStatusChanged(event);
                break;
            case PRODUCT_DELETED:
                handleProductDeleted(event);
                break;
            case VARIANT_PRICE_CHANGED:
                handleVariantPriceChanged(event);
                break;
            default:
                log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    private void handleProductCreated(ProductEvent event) {
        UUID productId = event.getProductId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        if (STATUS_ACTIVE.equals(product.getStatus())) {
            searchService.indexProduct(productId.toString());
            log.info("Product indexed after creation: {}", productId);
        } else {
            log.debug("Product not indexed - status is {}, not ACTIVE: {}", product.getStatus(), productId);
        }
    }

    private void handleProductUpdated(ProductEvent event) {
        UUID productId = event.getProductId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        if (STATUS_ACTIVE.equals(product.getStatus())) {
            // Re-index updated product
            searchService.indexProduct(productId.toString());
            log.info("Product re-indexed after update: {}", productId);
        } else {
            // If product is no longer ACTIVE, remove from index
            searchService.removeProductFromIndex(productId.toString());
            log.info("Product removed from index (no longer ACTIVE): {}", productId);
        }
    }

    private void handleProductStatusChanged(ProductEvent event) {
        UUID productId = event.getProductId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        if (STATUS_ACTIVE.equals(product.getStatus())) {
            // Product became ACTIVE, index it
            searchService.indexProduct(productId.toString());
            log.info("Product indexed after status change to ACTIVE: {}", productId);
        } else if (STATUS_ARCHIVED.equals(product.getStatus())) {
            // Product was archived/deleted, remove from index
            searchService.removeProductFromIndex(productId.toString());
            log.info("Product removed from index after archival: {}", productId);
        } else {
            // Product status changed but not to ACTIVE - remove if exists
            searchService.removeProductFromIndex(productId.toString());
            log.info("Product removed from index (status changed to {})", product.getStatus());
        }
    }

    private void handleProductDeleted(ProductEvent event) {
        UUID productId = event.getProductId();
        searchService.removeProductFromIndex(productId.toString());
        log.info("Product removed from index after deletion: {}", productId);
    }

    private void handleVariantPriceChanged(ProductEvent event) {
        UUID productId = event.getProductId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        // Re-index to update price range
        if (STATUS_ACTIVE.equals(product.getStatus())) {
            searchService.indexProduct(productId.toString());
            log.info("Product re-indexed after variant price change: {}", productId);
        }
    }

    private void handleEventProcessingError(ProductEvent event, Exception e) {
        log.error("Error processing event: id={}, type={}, productId={}", 
                event.getId(), event.getEventType(), event.getProductId(), e);

        event.markAsFailed(e.getMessage());
        event.incrementRetryCount();

        if (event.canRetry()) {
            log.info("Event will be retried later: id={}, retryCount={}", event.getId(), event.getRetryCount());
        } else {
            log.error("Event max retries exceeded, giving up: id={}", event.getId());
        }

        eventRepository.save(event);
    }
}
