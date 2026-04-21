package com.FXplore.fx_rate_service.validation;

import java.math.BigDecimal;

/**
 * Domain rule: in FX markets the bid (buy) price must always be
 * lower than the mid rate, which must be lower than the ask (sell) price.
 *
 * Kept in its own class so both the DTO layer (@AssertTrue in StoreRateRequest)
 * and the service layer (RateService) can enforce the same rule
 * without either depending on the other.
 */
public final class SpreadValidator {

    private SpreadValidator() {}

    /**
     * Returns true if bid < mid < ask.
     * Null values are treated as valid here — null checks are handled
     * separately by @NotNull at the DTO layer or explicit guards in the service.
     */
    public static boolean isValid(BigDecimal bid, BigDecimal mid, BigDecimal ask) {
        if (bid == null || mid == null || ask == null) {
            return true;
        }
        return bid.compareTo(mid) < 0 && mid.compareTo(ask) < 0;
    }
}

