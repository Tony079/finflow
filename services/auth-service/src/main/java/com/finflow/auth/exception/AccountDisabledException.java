package com.finflow.auth.exception;

public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("Account is disabled");
    }
}