package com.FXplore.fx_rate_service.exception;

public class RateProviderNotFoundException extends RuntimeException {

    public RateProviderNotFoundException(String message) {
        super(message);
    }

    public RateProviderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
