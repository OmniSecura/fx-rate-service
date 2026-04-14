package com.FXplore.fx_rate_service.exception;

public class CurrencyPairNotFoundException extends RuntimeException {

    public CurrencyPairNotFoundException(String message) {
        super(message);
    }

    public CurrencyPairNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
