package com.finflow.payment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String PAYMENT_EXCHANGE =
            "finflow.payment.exchange";

    public static final String PAYMENT_CREATED_QUEUE =
            "finflow.payment.created.queue";

    public static final String PAYMENT_CREATED_ROUTING_KEY =
            "payment.created";

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentCreatedQueue() {
        return new Queue(PAYMENT_CREATED_QUEUE, true);
    }

    @Bean
    public Binding paymentCreatedBinding(
            Queue paymentCreatedQueue,
            DirectExchange paymentExchange) {

        return BindingBuilder
                .bind(paymentCreatedQueue)
                .to(paymentExchange)
                .with(PAYMENT_CREATED_ROUTING_KEY);
    }
}