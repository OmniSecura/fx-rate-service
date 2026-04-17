package com.FXplore.fx_rate_service.dto;

import com.FXplore.fx_rate_service.model.EodFixing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for EOD fixing endpoints.
 */
public record EodFixingResponse(
        String pairCode,
        String providerCode,
        LocalDate fixingDate,
        BigDecimal fixingRate,
        String fixingTime,
        String fixingType,
        Boolean isOfficial,
        Instant publishedAt
) {
    public static EodFixingResponse from(EodFixing f) {
        return new EodFixingResponse(
                f.getPair().getPairCode(),
                f.getProvider().getProviderCode(),
                f.getFixingDate(),
                f.getFixingRate(),
                f.getFixingTime(),
                f.getFixingType(),
                f.getIsOfficial(),
                f.getPublishedAt()
        );
    }
}

