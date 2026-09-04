package com.finflow.fraud;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbit
public class FraudServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(FraudServiceApplication.class, args);
    }

}
