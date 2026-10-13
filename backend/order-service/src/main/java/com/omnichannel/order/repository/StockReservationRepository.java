package com.omnichannel.order.repository;

import com.omnichannel.order.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {
    
    List<StockReservation> findByOrderId(UUID orderId);
    
    List<StockReservation> findByOrderIdAndStatus(UUID orderId, String status);
    
    @Query("SELECT sr FROM StockReservation sr WHERE sr.status = :status AND sr.expiresAt < :now")
    List<StockReservation> findExpiredReservations(@Param("status") String status, @Param("now") Instant now);
}
