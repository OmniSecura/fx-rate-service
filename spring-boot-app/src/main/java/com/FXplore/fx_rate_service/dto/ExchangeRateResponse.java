package com.FXplore.fx_rate_service.dto;

import com.FXplore.fx_rate_service.model.ExchangeRate;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for exchange rate endpoints.
 * Avoids circular JSON serialisation caused by bidirectional JPA relationships.
 */
public record ExchangeRateResponse(
        String pairCode,
        String providerCode,
        BigDecimal bidRate,
        BigDecimal askRate,
        BigDecimal midRate,
        Instant rateTimestamp,
        String sourceSystem,
        Boolean isValid,
        Boolean isStale
) {
    public static ExchangeRateResponse from(ExchangeRate e) {
        return new ExchangeRateResponse(
                e.getPair().getPairCode(),
                e.getProvider().getProviderCode(),
                e.getBidRate(),
                e.getAskRate(),
                e.getMidRate(),
                e.getRateTimestamp(),
                e.getSourceSystem(),
                e.getIsValid(),
                e.getIsStale()
        );
    }
}

