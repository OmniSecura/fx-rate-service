package com.FXplore.fx_rate_service.service;

import com.FXplore.fx_rate_service.model.Currency;
import com.FXplore.fx_rate_service.model.CurrencyPair;
import com.FXplore.fx_rate_service.model.EodFixing;
import com.FXplore.fx_rate_service.model.ExchangeRate;
import com.FXplore.fx_rate_service.model.RateProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IRateService {
    // Look up a currency pair by code, throw if not found
    CurrencyPair getCurrencyPairByCode(String pairCode);

    // Look up a rate provider by code, throw if not found
    RateProvider getRateProviderByCode(String providerCode);

    // Accept a new rate (bid/ask/mid) for a currency pair and persist it
    void storeRate(String pairCode, String providerCode, BigDecimal bid, BigDecimal ask, BigDecimal mid);

    // Return the most recent rate for a pair, with a staleness flag if older than 4 hours
    Optional<ExchangeRate> getLatestRate(String pairCode);

    // Return rate history for a pair between two dates
    List<ExchangeRate> getRateHistory(String pairCode, LocalDate from, LocalDate to);

    // Derive the implied rate for pair A/C given rates A/B and B/C
    Optional<BigDecimal> calculateCrossRate(String pairCodeAC, String pairCodeAB, String pairCodeBC);

    // Convert a given amount from one currency to another using the latest mid rate
    Optional<BigDecimal> convertAmount(BigDecimal amount, String pairCode);

    // Return the official end-of-day fixing for a pair on a given date
    Optional<EodFixing> getEodFixing(String pairCode, LocalDate date);

    // Return the latest rate per pair where the rate is older than 4 hours
    List<ExchangeRate> getStaleRates();

    // Return all active currencies
    List<Currency> getAllActiveCurrencies();
}
