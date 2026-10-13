package com.omnichannel.notification.service;

import com.omnichannel.notification.dto.request.CreateNotificationRequest;
import com.omnichannel.notification.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationResponse createNotification(UUID customerId, CreateNotificationRequest request);

    NotificationResponse getNotificationById(UUID id);

    Page<NotificationResponse> getNotificationsByCustomer(UUID customerId, Pageable pageable);

    Page<NotificationResponse> getNotificationsByStatus(String status, Pageable pageable);

    long getUnreadCount(UUID customerId);

    NotificationResponse markAsRead(UUID id);

    Page<NotificationResponse> getUnreadNotifications(UUID customerId, Pageable pageable);

    void deleteNotification(UUID id);
}

