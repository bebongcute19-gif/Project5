package com.omnichannel.product.scheduler;

import com.omnichannel.product.service.ProductSearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ProductSyncScheduler {

    private final ProductSearchSyncService productSearchSyncService;

    /**
     * Process pending events and sync to Elasticsearch every 5 seconds
     * Batch size: 10 events per batch
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void syncPendingEvents() {
        try {
            log.debug("Starting scheduled sync of pending product events");
            productSearchSyncService.processPendingEvents(10);
        } catch (Exception e) {
            log.error("Error in scheduled sync of pending events", e);
        }
    }
}
