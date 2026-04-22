package com.FXplore.fx_rate_service.dto;

import com.FXplore.fx_rate_service.model.Currency;

/**
 * Response DTO for currency endpoints.
 */
public record CurrencyResponse(
        Integer id,
        String isoCode,
        String currencyName,
        String country,
        String numericCode,
        Short minorUnits,
        Boolean isActive,
        String region
) {
    public static CurrencyResponse from(Currency c) {
        return new CurrencyResponse(
                c.getId(),
                c.getIsoCode(),
                c.getCurrencyName(),
                c.getCountry(),
                c.getNumericCode(),
                c.getMinorUnits(),
                c.getIsActive(),
                c.getRegion()
        );
    }
}

