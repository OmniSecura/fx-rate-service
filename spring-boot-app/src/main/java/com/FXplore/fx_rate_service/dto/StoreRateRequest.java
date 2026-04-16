package com.FXplore.fx_rate_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request body for POST /api/rates
 */
public record StoreRateRequest(

        @NotBlank(message = "pairCode is required")
        String pairCode,

        @NotBlank(message = "providerCode is required")
        String providerCode,

        @NotNull(message = "bid is required")
        @Positive(message = "bid must be positive")
        BigDecimal bid,

        @NotNull(message = "ask is required")
        @Positive(message = "ask must be positive")
        BigDecimal ask,

        @NotNull(message = "mid is required")
        @Positive(message = "mid must be positive")
        BigDecimal mid
) {}
