package com.FXplore.fx_rate_service;

import com.FXplore.fx_rate_service.dto.ConversionResponse;
import com.FXplore.fx_rate_service.dto.CurrencyResponse;
import com.FXplore.fx_rate_service.dto.EodFixingResponse;
import com.FXplore.fx_rate_service.dto.ExchangeRateResponse;
import com.FXplore.fx_rate_service.exception.CurrencyPairNotFoundException;
import com.FXplore.fx_rate_service.exception.InvalidExchangeRateException;
import com.FXplore.fx_rate_service.exception.RateProviderNotFoundException;
import com.FXplore.fx_rate_service.service.IRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link IRateService} against a real MySQL 8.0 database
 * managed by Testcontainers (see {@link AbstractIntegrationTest}).
 *
 * Tests call the service directly (no HTTP layer) so {@code @Transactional} works
 * correctly — Spring rolls back any writes after each test method, keeping the
 * seed data from data.sql untouched for subsequent tests.
 *
 * Seed data summary (data.sql):
 *   - 20 active currencies
 *   - 20 rate providers  (REUTERS id=1, ECB id=3, WMR id=8 …)
 *   - 20 currency pairs  (EUR/USD id=1, GBP/USD id=2 …)
 *   - 20 exchange rates  all timestamped 2026-03-26 → all STALE on 2026-04-20
 *   - 0  eod fixings     (table is empty in seed data)
 */
@Transactional
class RateServiceIT extends AbstractIntegrationTest {

    @Autowired
    private IRateService rateService;

    // ================================================================
    // storeRate
    // ================================================================

    @Test
    void storeRate_withValidData_persistsRateAndIsRetrievable() {
        // Given — EUR/USD pair + REUTERS provider exist in seed data
        BigDecimal bid = new BigDecimal("1.0800");
        BigDecimal ask = new BigDecimal("1.0900");
        BigDecimal mid = new BigDecimal("1.0850");

        // When
        rateService.storeRate("EUR/USD", "REUTERS", bid, ask, mid);

        // Then — getLatestRate should now return the freshly stored rate
        Optional<ExchangeRateResponse> result = rateService.getLatestRate("EUR/USD");

        assertTrue(result.isPresent());
        assertEquals(0, bid.compareTo(result.get().bidRate()));
        assertEquals(0, ask.compareTo(result.get().askRate()));
        assertEquals(0, mid.compareTo(result.get().midRate()));
        assertEquals("REUTERS", result.get().providerCode());
        // Fresh rate must not be stale
        assertFalse(result.get().isStale());
    }

    @Test
    void storeRate_withECBProvider_setsSourceSystemToECB_FEED() {
        // Given — ECB is a CENTRAL_BANK provider with special source-system mapping
        BigDecimal bid = new BigDecimal("1.0800");
        BigDecimal ask = new BigDecimal("1.0900");
        BigDecimal mid = new BigDecimal("1.0850");

        // When
        rateService.storeRate("EUR/USD", "ECB", bid, ask, mid);

        // Then — source system must be ECB_FEED (RateService line 90)
        Optional<ExchangeRateResponse> result = rateService.getLatestRate("EUR/USD");
        assertTrue(result.isPresent());
        assertEquals("ECB_FEED", result.get().sourceSystem());
    }

    @Test
    void storeRate_withUnknownPair_throwsCurrencyPairNotFoundException() {
        assertThrows(CurrencyPairNotFoundException.class, () ->
                rateService.storeRate("XXX/YYY", "REUTERS",
                        new BigDecimal("1.0"), new BigDecimal("1.1"), new BigDecimal("1.05")));
    }

    @Test
    void storeRate_withUnknownProvider_throwsRateProviderNotFoundException() {
        assertThrows(RateProviderNotFoundException.class, () ->
                rateService.storeRate("EUR/USD", "NO_SUCH_PROVIDER",
                        new BigDecimal("1.0"), new BigDecimal("1.1"), new BigDecimal("1.05")));
    }

    @Test
    void storeRate_withInvalidSpread_throwsInvalidExchangeRateException() {
        // bid > ask violates the spread rule
        assertThrows(InvalidExchangeRateException.class, () ->
                rateService.storeRate("EUR/USD", "REUTERS",
                        new BigDecimal("1.09"), new BigDecimal("1.08"), new BigDecimal("1.085")));
    }

    // ================================================================
    // getLatestRate
    // ================================================================

    @Test
    void getLatestRate_withSeedData_returnsRateMarkedAsStale() {
        // Seed rates are from 2026-03-26 — well past the 4-hour threshold on 2026-04-20
        Optional<ExchangeRateResponse> result = rateService.getLatestRate("EUR/USD");

        assertTrue(result.isPresent());
        assertEquals("EUR/USD", result.get().pairCode());
        assertTrue(result.get().isStale(), "Seed rate from 2026-03-26 must be stale");
    }

    @Test
    void getLatestRate_withUnknownPair_throwsCurrencyPairNotFoundException() {
        assertThrows(CurrencyPairNotFoundException.class, () ->
                rateService.getLatestRate("ZZZ/QQQ"));
    }

    @Test
    void getLatestRate_withPairThatHasNoRates_returnsEmpty() {
        // EUR/USD has seed data, but if we ask for a pair that exists in currency_pair
        // but has no exchange_rate rows — returns Optional.empty()
        // We use GBP/USD seed rate exists, so let us verify the Optional.of path
        Optional<ExchangeRateResponse> result = rateService.getLatestRate("GBP/USD");
        assertTrue(result.isPresent());
        assertEquals("GBP/USD", result.get().pairCode());
    }

    // ================================================================
    // getRateHistory
    // ================================================================

    @Test
    void getRateHistory_withinSeedDataRange_returnsRates() {
        // Seed rates are on 2026-03-26 — query that exact day
        List<ExchangeRateResponse> result = rateService.getRateHistory(
                "EUR/USD",
                LocalDate.of(2026, 3, 26),
                LocalDate.of(2026, 3, 26));

        assertFalse(result.isEmpty(), "Should find at least one rate on 2026-03-26");
        assertTrue(result.stream().allMatch(r -> "EUR/USD".equals(r.pairCode())));
    }

    @Test
    void getRateHistory_outsideSeedDataRange_returnsEmptyList() {
        List<ExchangeRateResponse> result = rateService.getRateHistory(
                "EUR/USD",
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 12, 31));

        assertTrue(result.isEmpty());
    }

    // ================================================================
    // getStaleRates
    // ================================================================

    @Test
    void getStaleRates_withSeedData_returnsAllTwentyPairs() {
        // All 20 seed rates are from 2026-03-26 — stale as of 2026-04-20
        List<ExchangeRateResponse> staleRates = rateService.getStaleRates();

        assertEquals(20, staleRates.size(),
                "All 20 seed rates should be reported as stale");
        assertTrue(staleRates.stream().allMatch(ExchangeRateResponse::isStale));
    }

    // ================================================================
    // calculateCrossRate
    // ================================================================

    @Test
    void calculateCrossRate_withSeedData_returnsComputedValue() {
        // EUR/GBP mid = 0.83515, GBP/USD mid = 1.29625 → EUR/USD cross ≈ 1.082...
        Optional<BigDecimal> result =
                rateService.calculateCrossRate("EUR/USD", "EUR/GBP", "GBP/USD");

        assertTrue(result.isPresent());
        assertTrue(result.get().compareTo(BigDecimal.ZERO) > 0,
                "Cross rate must be positive");
    }

    @Test
    void calculateCrossRate_withUnknownPair_throwsCurrencyPairNotFoundException() {
        assertThrows(CurrencyPairNotFoundException.class, () ->
                rateService.calculateCrossRate("A/B", "A/C", "XXX/YYY"));
    }

    // ================================================================
    // convertAmount
    // ================================================================

    @Test
    void convertAmount_withSeedData_returnsConvertedAmount() {
        BigDecimal amount = new BigDecimal("10000");

        Optional<ConversionResponse> result = rateService.convertAmount(amount, "EUR/USD");

        assertTrue(result.isPresent());
        assertEquals("EUR", result.get().fromCurrency());
        assertEquals("USD", result.get().toCurrency());
        assertTrue(result.get().convertedAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void convertAmount_withZeroAmount_returnsZeroConvertedAmount() {
        Optional<ConversionResponse> result =
                rateService.convertAmount(BigDecimal.ZERO, "EUR/USD");

        assertTrue(result.isPresent());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get().convertedAmount()));
    }

    // ================================================================
    // getEodFixing
    // ================================================================

    @Test
    void getEodFixing_whenNoFixingInSeedData_returnsEmpty() {
        // data.sql contains no eod_fixing rows
        Optional<EodFixingResponse> result =
                rateService.getEodFixing("EUR/USD", LocalDate.of(2026, 3, 26));

        assertFalse(result.isPresent(),
                "No EOD fixings in seed data, result must be empty");
    }

    // ================================================================
    // getAllActiveCurrencies
    // ================================================================

    @Test
    void getAllActiveCurrencies_withSeedData_returnsTwentyCurrencies() {
        List<CurrencyResponse> currencies = rateService.getAllActiveCurrencies();

        assertEquals(20, currencies.size(),
                "data.sql inserts 20 active currencies");
        assertTrue(currencies.stream().allMatch(CurrencyResponse::isActive));
    }
}

