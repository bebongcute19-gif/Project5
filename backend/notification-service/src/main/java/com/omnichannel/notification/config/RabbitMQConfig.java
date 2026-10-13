package com.omnichannel.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String DLX_EXCHANGE = "dlx.exchange";

    // Notification Queues
    public static final String NOTIFICATION_ORDER_CREATED_QUEUE = "notification.order-created.queue";
    public static final String NOTIFICATION_ORDER_CONFIRMED_QUEUE = "notification.order-confirmed.queue";
    public static final String NOTIFICATION_PAYMENT_SUCCEEDED_QUEUE = "notification.payment-succeeded.queue";
    public static final String NOTIFICATION_PAYMENT_FAILED_QUEUE = "notification.payment-failed.queue";

    // Exchanges
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    // Notification Queues
    @Bean
    public Queue notificationOrderCreatedQueue() {
        return QueueBuilder.durable(NOTIFICATION_ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dead-letter")
                .build();
    }

    @Bean
    public Queue notificationOrderConfirmedQueue() {
        return QueueBuilder.durable(NOTIFICATION_ORDER_CONFIRMED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Queue notificationPaymentSucceededQueue() {
        return QueueBuilder.durable(NOTIFICATION_PAYMENT_SUCCEEDED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Queue notificationPaymentFailedQueue() {
        return QueueBuilder.durable(NOTIFICATION_PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .build();
    }

    // Bindings
    @Bean
    public Binding bindingNotificationOrderCreated(Queue notificationOrderCreatedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(notificationOrderCreatedQueue)
                .to(orderExchange)
                .with("order.created");
    }

    @Bean
    public Binding bindingNotificationOrderConfirmed(Queue notificationOrderConfirmedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(notificationOrderConfirmedQueue)
                .to(orderExchange)
                .with("order.confirmed");
    }

    @Bean
    public Binding bindingNotificationPaymentSucceeded(Queue notificationPaymentSucceededQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(notificationPaymentSucceededQueue)
                .to(paymentExchange)
                .with("payment.succeeded");
    }

    @Bean
    public Binding bindingNotificationPaymentFailed(Queue notificationPaymentFailedQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(notificationPaymentFailedQueue)
                .to(paymentExchange)
                .with("payment.failed");
    }
}
