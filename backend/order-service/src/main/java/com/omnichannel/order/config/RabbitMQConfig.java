package com.omnichannel.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String DLX_EXCHANGE = "dlx.exchange";

    public static final String ORDER_PAYMENT_SUCCEEDED_QUEUE = "order.payment-succeeded.queue";
    public static final String ORDER_PAYMENT_FAILED_QUEUE = "order.payment-failed.queue";
    public static final String DEAD_LETTER_QUEUE = "dead-letter.queue";

    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.succeeded";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderPaymentSucceededQueue() {
        return QueueBuilder.durable(ORDER_PAYMENT_SUCCEEDED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dead-letter")
                .build();
    }

    @Bean
    public Queue orderPaymentFailedQueue() {
        return QueueBuilder.durable(ORDER_PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dead-letter")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding bindingOrderPaymentSucceeded(Queue orderPaymentSucceededQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(orderPaymentSucceededQueue)
                .to(paymentExchange)
                .with(PAYMENT_SUCCEEDED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange("payment.exchange", true, false);
    }

    @Bean
    public Binding bindingOrderPaymentFailed(Queue orderPaymentFailedQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(orderPaymentFailedQueue)
                .to(paymentExchange)
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding bindingDeadLetter(Queue deadLetterQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(dlxExchange)
                .with("dead-letter");
    }
}
