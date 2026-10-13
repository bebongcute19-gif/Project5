package com.omnichannel.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichannel.notification.dto.request.CreateNotificationRequest;
import com.omnichannel.notification.event.NotificationEvent;
import com.omnichannel.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "notification.order-created.queue")
    public void handleOrderCreated(String message) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .customerId(event.getCustomerId())
                    .eventType("ORDER_CREATED")
                    .title("Order Created")
                    .message("Your order #" + event.getOrderNumber() + " has been created successfully")
                    .relatedEntityId(event.getOrderId())
                    .entityType("ORDER")
                    .build();
            
            notificationService.createNotification(event.getCustomerId(), request);
            log.info("Notification created for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error handling OrderCreatedEvent", e);
        }
    }

    @RabbitListener(queues = "notification.order-confirmed.queue")
    public void handleOrderConfirmed(String message) {
        try {
            OrderConfirmedEvent event = objectMapper.readValue(message, OrderConfirmedEvent.class);
            
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .customerId(event.getCustomerId())
                    .eventType("ORDER_CONFIRMED")
                    .title("Order Confirmed")
                    .message("Your order #" + event.getOrderNumber() + " has been confirmed")
                    .relatedEntityId(event.getOrderId())
                    .entityType("ORDER")
                    .build();
            
            notificationService.createNotification(event.getCustomerId(), request);
            log.info("Notification created for confirmed order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error handling OrderConfirmedEvent", e);
        }
    }
}
