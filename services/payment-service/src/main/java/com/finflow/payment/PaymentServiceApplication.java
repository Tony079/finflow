package com.finflow.payment;

import com.finflow.payment.service.RedisService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

@EnableScheduling
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(PaymentServiceApplication.class, args);
    }

}