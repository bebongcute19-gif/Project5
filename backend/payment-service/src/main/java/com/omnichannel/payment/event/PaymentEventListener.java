package com.omnichannel.payment.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichannel.payment.entity.Payment;
import com.omnichannel.payment.entity.ProcessedEvent;
import com.omnichannel.payment.repository.PaymentRepository;
import com.omnichannel.payment.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "payment.order-created.queue")
    @Transactional
    public void handleOrderCreated(String message) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            
            // Check if payment already created (idempotency)
            String eventId = event.getOrderId().toString() + "-order-created";
            if (processedEventRepository.existsById(eventId)) {
                log.info("Order creation event already processed: {}", eventId);
                return;
            }

            // Payment already created during order creation, just log
            log.info("Order {} created event received in Payment Service", event.getOrderId());

            // Mark event as processed
            ProcessedEvent processed = ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType("ORDER_CREATED")
                    .processedAt(Instant.now())
                    .build();
            processedEventRepository.save(processed);
        } catch (Exception e) {
            log.error("Error handling OrderCreatedEvent", e);
            throw new RuntimeException("Failed to handle order created event", e);
        }
    }
}
