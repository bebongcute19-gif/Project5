package com.omnichannel.notification.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private UUID customerId;
    private String eventType; // ORDER_CREATED, PAYMENT_SUCCEEDED, PAYMENT_FAILED, ORDER_CONFIRMED
    private String title;
    private String message;
    private UUID relatedEntityId; // orderId or paymentId
    private String entityType; // ORDER or PAYMENT

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Instant timestamp;
}
