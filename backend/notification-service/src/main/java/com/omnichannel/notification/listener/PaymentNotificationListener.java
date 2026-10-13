package com.omnichannel.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichannel.notification.dto.request.CreateNotificationRequest;
import com.omnichannel.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentNotificationListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "notification.payment-succeeded.queue")
    public void handlePaymentSucceeded(String message) {
        try {
            PaymentSucceededEvent event = objectMapper.readValue(message, PaymentSucceededEvent.class);
            
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .customerId(event.getCustomerId())
                    .eventType("PAYMENT_SUCCEEDED")
                    .title("Payment Successful")
                    .message("Payment for order has been processed successfully")
                    .relatedEntityId(event.getPaymentId())
                    .entityType("PAYMENT")
                    .build();
            
            notificationService.createNotification(event.getCustomerId(), request);
            log.info("Notification created for successful payment: {}", event.getPaymentId());
        } catch (Exception e) {
            log.error("Error handling PaymentSucceededEvent", e);
        }
    }

    @RabbitListener(queues = "notification.payment-failed.queue")
    public void handlePaymentFailed(String message) {
        try {
            PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
            
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .customerId(event.getCustomerId())
                    .eventType("PAYMENT_FAILED")
                    .title("Payment Failed")
                    .message("Payment processing failed: " + event.getFailureReason())
                    .relatedEntityId(event.getPaymentId())
                    .entityType("PAYMENT")
                    .build();
            
            notificationService.createNotification(event.getCustomerId(), request);
            log.info("Notification created for failed payment: {}", event.getPaymentId());
        } catch (Exception e) {
            log.error("Error handling PaymentFailedEvent", e);
        }
    }
}
