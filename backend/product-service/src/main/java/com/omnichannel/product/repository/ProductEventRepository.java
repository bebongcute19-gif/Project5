package com.omnichannel.product.repository;

import com.omnichannel.product.entity.ProductEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductEventRepository extends JpaRepository<ProductEvent, UUID> {

    @Query("SELECT pe FROM ProductEvent pe WHERE pe.processed = false AND pe.retryCount < pe.maxRetries ORDER BY pe.createdAt ASC")
    Page<ProductEvent> findUnprocessedEvents(Pageable pageable);

    @Query("SELECT pe FROM ProductEvent pe WHERE pe.processed = false AND pe.retryCount < pe.maxRetries ORDER BY pe.createdAt ASC")
    List<ProductEvent> findUnprocessedEvents();

    Page<ProductEvent> findByProductId(UUID productId, Pageable pageable);

    Page<ProductEvent> findByProcessed(Boolean processed, Pageable pageable);

    @Query("SELECT pe FROM ProductEvent pe WHERE pe.createdAt < :cutoffTime AND pe.processed = true ORDER BY pe.createdAt DESC")
    List<ProductEvent> findProcessedEventsOlderThan(@Param("cutoffTime") Instant cutoffTime);
}
