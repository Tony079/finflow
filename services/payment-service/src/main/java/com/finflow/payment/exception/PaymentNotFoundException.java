package com.finflow.payment.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException() {
        super("Payment not found");
    }
}