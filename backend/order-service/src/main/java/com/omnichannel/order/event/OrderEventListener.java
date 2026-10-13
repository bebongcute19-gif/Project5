package com.omnichannel.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichannel.order.constant.OrderStatus;
import com.omnichannel.order.entity.Order;
import com.omnichannel.order.entity.ProcessedEvent;
import com.omnichannel.order.repository.OrderRepository;
import com.omnichannel.order.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "order.payment-succeeded.queue")
    @Transactional
    public void handlePaymentSucceeded(String message) {
        try {
            PaymentSucceededEvent event = objectMapper.readValue(message, PaymentSucceededEvent.class);
            
            // Check if already processed (idempotency)
            String eventId = event.getPaymentId().toString() + "-payment-succeeded";
            if (processedEventRepository.existsById(eventId)) {
                log.info("Event already processed: {}", eventId);
                return;
            }

            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

            // Update order status to CONFIRMED
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            // Mark event as processed
            ProcessedEvent processed = ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType("PAYMENT_SUCCEEDED")
                    .processedAt(Instant.now())
                    .build();
            processedEventRepository.save(processed);

            log.info("Order {} confirmed after successful payment", event.getOrderId());
        } catch (Exception e) {
            log.error("Error handling PaymentSucceededEvent", e);
            throw new RuntimeException("Failed to handle payment succeeded event", e);
        }
    }

    @RabbitListener(queues = "order.payment-failed.queue")
    @Transactional
    public void handlePaymentFailed(String message) {
        try {
            PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
            
            // Check if already processed
            String eventId = event.getPaymentId().toString() + "-payment-failed";
            if (processedEventRepository.existsById(eventId)) {
                log.info("Event already processed: {}", eventId);
                return;
            }

            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

            // Update order status to FAILED
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);

            // Mark event as processed
            ProcessedEvent processed = ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType("PAYMENT_FAILED")
                    .processedAt(Instant.now())
                    .build();
            processedEventRepository.save(processed);

            log.info("Order {} marked as failed after payment failure", event.getOrderId());
        } catch (Exception e) {
            log.error("Error handling PaymentFailedEvent", e);
            throw new RuntimeException("Failed to handle payment failed event", e);
        }
    }
}
