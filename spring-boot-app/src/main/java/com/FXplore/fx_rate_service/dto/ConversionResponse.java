package com.FXplore.fx_rate_service.dto;

import java.math.BigDecimal;

/**
 * Response DTO for currency conversion endpoint.
 */
public record ConversionResponse(
        String fromCurrency,
        String toCurrency,
        BigDecimal amount,
        BigDecimal convertedAmount,
        BigDecimal rate,
        String pairCode
) {}

