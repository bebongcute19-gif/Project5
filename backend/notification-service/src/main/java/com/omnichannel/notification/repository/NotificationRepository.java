package com.omnichannel.notification.repository;

import com.omnichannel.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Notification> findByStatus(String status, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.customerId = ?1 AND n.isRead = false")
    long countUnreadByCustomerId(UUID customerId);

    Page<Notification> findByCustomerIdAndIsRead(UUID customerId, Boolean isRead, Pageable pageable);

    Page<Notification> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
}

