package com.FXplore.fx_rate_service.exception;

public class InvalidExchangeRateException extends RuntimeException {

    public InvalidExchangeRateException(String message) {
        super(message);
    }

    public InvalidExchangeRateException(String message, Throwable cause) {
        super(message, cause);
    }
}
