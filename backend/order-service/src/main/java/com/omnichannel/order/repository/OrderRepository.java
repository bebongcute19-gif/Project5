package com.omnichannel.order.repository;

import com.omnichannel.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    Page<Order> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);
    
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    Page<Order> findAllOrders(Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.status = :status")
    Page<Order> findByStatus(@Param("status") String status, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId AND o.status = :status")
    Page<Order> findByCustomerIdAndStatus(@Param("customerId") UUID customerId, @Param("status") String status, Pageable pageable);
}
