package com.omnichannel.notification.service.impl;

import com.omnichannel.notification.dto.request.CreateNotificationRequest;
import com.omnichannel.notification.dto.response.NotificationResponse;
import com.omnichannel.notification.entity.Notification;
import com.omnichannel.notification.exception.ResourceNotFoundException;
import com.omnichannel.notification.repository.NotificationRepository;
import com.omnichannel.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public NotificationResponse createNotification(UUID customerId, CreateNotificationRequest request) {
        Notification notification = Notification.builder()
                .customerId(customerId)
                .eventType(request.getEventType())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedEntityId(request.getRelatedEntityId())
                .entityType(request.getEntityType())
                .status("UNREAD")
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created for customer: {}", customerId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));

        return mapToResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByCustomer(UUID customerId, Pageable pageable) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByStatus(String status, Pageable pageable) {
        return notificationRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID customerId) {
        return notificationRepository.countUnreadByCustomerId(customerId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));

        notification.setIsRead(true);
        notification.setStatus("READ");
        Notification updated = notificationRepository.save(notification);

        log.info("Notification {} marked as read", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotifications(UUID customerId, Pageable pageable) {
        return notificationRepository.findByCustomerIdAndIsRead(customerId, false, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID id) {
        if (!notificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notification not found: " + id);
        }
        notificationRepository.deleteById(id);
        log.info("Notification {} deleted", id);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .customerId(notification.getCustomerId())
                .eventType(notification.getEventType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .isRead(notification.getIsRead())
                .relatedEntityId(notification.getRelatedEntityId())
                .entityType(notification.getEntityType())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
