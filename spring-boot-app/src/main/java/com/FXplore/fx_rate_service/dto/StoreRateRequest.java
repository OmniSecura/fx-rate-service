package com.FXplore.fx_rate_service.dto;

import com.FXplore.fx_rate_service.validation.SpreadValidator;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request body for POST /api/rates.
 *
 * FX spread constraint: bid < mid < ask
 * In FX markets the bid (buy) price is always lower than the ask (sell) price,
 * with the mid rate sitting exactly between them.
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
) {
    /**
     * Validates the FX bid/ask spread: bid < mid < ask.
     * Called automatically by Bean Validation when @Valid is present on the controller method.
     */
    @AssertTrue(message = "Bid/ask spread invalid: required bid < mid < ask")
    public boolean isSpreadValid() {
        return SpreadValidator.isValid(bid, mid, ask);
    }
}
