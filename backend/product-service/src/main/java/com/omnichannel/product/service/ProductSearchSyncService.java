package com.omnichannel.product.service;

public interface ProductSearchSyncService {

    /**
     * Process pending outbox events and sync to Elasticsearch
     */
    void processPendingEvents();

    /**
     * Process a specific number of pending events
     *
     * @param batchSize Number of events to process in one batch
     */
    void processPendingEvents(int batchSize);
}
