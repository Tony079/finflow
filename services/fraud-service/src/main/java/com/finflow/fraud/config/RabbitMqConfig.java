package com.finflow.fraud.config;

import org.springframework.amqp.core.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String PAYMENT_EXCHANGE =
            "finflow.payment.exchange";

    public static final String PAYMENT_CREATED_QUEUE =
            "fraud.payment.created.queue";

    public static final String PAYMENT_CREATED_ROUTING_KEY =
            "payment.created";

    @Bean
    public Queue paymentCreatedQueue(AmqpAdmin amqpAdmin) {
        Queue queue = new Queue(PAYMENT_CREATED_QUEUE, true);
        amqpAdmin.declareQueue(queue);
        return queue;
    }

    @Bean
    public DirectExchange paymentExchange(AmqpAdmin amqpAdmin) {
        DirectExchange exchange =
                new DirectExchange(PAYMENT_EXCHANGE, true, false);

        amqpAdmin.declareExchange(exchange);

        return exchange;
    }

    @Bean
    public Binding paymentCreatedBinding(
            Queue paymentCreatedQueue,
            DirectExchange paymentExchange,
            AmqpAdmin amqpAdmin) {

        Binding binding = BindingBuilder
                .bind(paymentCreatedQueue)
                .to(paymentExchange)
                .with(PAYMENT_CREATED_ROUTING_KEY);

        amqpAdmin.declareBinding(binding);

        return binding;
    }

}