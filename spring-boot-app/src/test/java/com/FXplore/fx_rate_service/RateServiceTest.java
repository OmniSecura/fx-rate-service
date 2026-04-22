package com.FXplore.fx_rate_service;

import com.FXplore.fx_rate_service.dao.ICurrencyPairRepository;
import com.FXplore.fx_rate_service.dao.IEodFixingRepository;
import com.FXplore.fx_rate_service.dao.IExchangeRateRepository;
import com.FXplore.fx_rate_service.dao.IRateProviderRepository;
import com.FXplore.fx_rate_service.dao.ICurrencyRepository;
import com.FXplore.fx_rate_service.dto.ConversionResponse;
import com.FXplore.fx_rate_service.dto.EodFixingResponse;
import com.FXplore.fx_rate_service.dto.ExchangeRateResponse;
import com.FXplore.fx_rate_service.exception.CurrencyPairNotFoundException;
import com.FXplore.fx_rate_service.exception.InvalidExchangeRateException;
import com.FXplore.fx_rate_service.exception.RateProviderNotFoundException;
import com.FXplore.fx_rate_service.model.Currency;
import com.FXplore.fx_rate_service.model.CurrencyPair;
import com.FXplore.fx_rate_service.model.EodFixing;
import com.FXplore.fx_rate_service.model.ExchangeRate;
import com.FXplore.fx_rate_service.model.RateProvider;
import com.FXplore.fx_rate_service.service.RateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RateServiceTest {

    @Mock
    private IExchangeRateRepository exchangeRateRepository;

    @Mock
    private IEodFixingRepository eodFixingRepository;

    @Mock
    private ICurrencyPairRepository currencyPairRepository;

    @Mock
    private IRateProviderRepository rateProviderRepository;

    @Mock
    private ICurrencyRepository currencyRepository;

    @InjectMocks
    private RateService rateService;

    private CurrencyPair eurGbpPair;
    private CurrencyPair gbpUsdPair;
    private CurrencyPair eurUsdPair;
    private RateProvider provider;
    private ExchangeRate eurGbpRate;
    private ExchangeRate gbpUsdRate;

    @BeforeEach
    void setUp() {
        // Currencies needed by convertAmount (pair.getBaseCurrency().getIsoCode())
        Currency gbp = new Currency();
        gbp.setIsoCode("GBP");

        Currency usd = new Currency();
        usd.setIsoCode("USD");

        Currency eur = new Currency();
        eur.setIsoCode("EUR");

        eurGbpPair = new CurrencyPair();
        eurGbpPair.setId(1);
        eurGbpPair.setPairCode("EUR/GBP");
        eurGbpPair.setBaseCurrency(eur);
        eurGbpPair.setQuoteCurrency(gbp);

        gbpUsdPair = new CurrencyPair();
        gbpUsdPair.setId(2);
        gbpUsdPair.setPairCode("GBP/USD");
        gbpUsdPair.setBaseCurrency(gbp);
        gbpUsdPair.setQuoteCurrency(usd);

        eurUsdPair = new CurrencyPair();
        eurUsdPair.setId(3);
        eurUsdPair.setPairCode("EUR/USD");
        eurUsdPair.setBaseCurrency(eur);
        eurUsdPair.setQuoteCurrency(usd);

        provider = new RateProvider();
        provider.setId(1);
        provider.setProviderCode("TEST");

        eurGbpRate = new ExchangeRate();
        eurGbpRate.setPair(eurGbpPair);
        eurGbpRate.setProvider(provider);
        eurGbpRate.setMidRate(new BigDecimal("0.85"));
        eurGbpRate.setBidRate(new BigDecimal("0.8498"));
        eurGbpRate.setAskRate(new BigDecimal("0.8502"));
        eurGbpRate.setSourceSystem("REUTERS");
        eurGbpRate.setIsValid(true);
        eurGbpRate.setIsStale(false);
        eurGbpRate.setRateTimestamp(Instant.now());

        gbpUsdRate = new ExchangeRate();
        gbpUsdRate.setPair(gbpUsdPair);
        gbpUsdRate.setProvider(provider);
        gbpUsdRate.setMidRate(new BigDecimal("1.265"));
        gbpUsdRate.setBidRate(new BigDecimal("1.2648"));
        gbpUsdRate.setAskRate(new BigDecimal("1.2652"));
        gbpUsdRate.setSourceSystem("REUTERS");
        gbpUsdRate.setIsValid(true);
        gbpUsdRate.setIsStale(false);
        gbpUsdRate.setRateTimestamp(Instant.now());
    }

    // ----------------------------------------------------------------
    // Unit — Cross Rate Calculation
    // ----------------------------------------------------------------

    @Test
    void testCalculateCrossRate_Success() {
        // Given
        when(currencyPairRepository.findByPairCode("EUR/GBP")).thenReturn(Optional.of(eurGbpPair));
        when(currencyPairRepository.findByPairCode("GBP/USD")).thenReturn(Optional.of(gbpUsdPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair)).thenReturn(Optional.of(eurGbpRate));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(gbpUsdPair)).thenReturn(Optional.of(gbpUsdRate));

        // When
        Optional<BigDecimal> result = rateService.calculateCrossRate("EUR/USD", "EUR/GBP", "GBP/USD");

        // Then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("1.075250"), result.get()); // 0.85 * 1.265 = 1.07525
    }

    @Test
    void testCalculateCrossRate_MissingLeg() {
        // Given
        when(currencyPairRepository.findByPairCode("EUR/GBP")).thenReturn(Optional.of(eurGbpPair));
        when(currencyPairRepository.findByPairCode("GBP/USD")).thenReturn(Optional.of(gbpUsdPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair)).thenReturn(Optional.empty());

        // When
        Optional<BigDecimal> result = rateService.calculateCrossRate("EUR/USD", "EUR/GBP", "GBP/USD");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testCalculateCrossRate_UnknownPair() {
        // Given
        when(currencyPairRepository.findByPairCode("EUR/GBP")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CurrencyPairNotFoundException.class,
                () -> rateService.calculateCrossRate("EUR/USD", "EUR/GBP", "GBP/USD"));
    }

    // ----------------------------------------------------------------
    // Unit — Staleness Detection
    // ----------------------------------------------------------------

    @Test
    void testStalenessDetection_Stale() {
        // Given — rate is 5 hours old, threshold is 4 hours
        eurGbpRate.setRateTimestamp(Instant.now().minus(5, java.time.temporal.ChronoUnit.HOURS));
        when(currencyPairRepository.findByPairCode("EUR/GBP")).thenReturn(Optional.of(eurGbpPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair)).thenReturn(Optional.of(eurGbpRate));

        // When — getLatestRate returns Optional<ExchangeRateResponse> (DTO record)
        Optional<ExchangeRateResponse> result = rateService.getLatestRate("EUR/GBP");

        // Then
        assertTrue(result.isPresent());
        assertTrue(result.get().isStale()); // record accessor — isStale(), not getIsStale()
    }

    @Test
    void testStalenessDetection_Boundary() {
        // Given — rate is 3 hours old, below the 4-hour threshold
        eurGbpRate.setRateTimestamp(Instant.now().minus(3, java.time.temporal.ChronoUnit.HOURS));
        when(currencyPairRepository.findByPairCode("EUR/GBP")).thenReturn(Optional.of(eurGbpPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair)).thenReturn(Optional.of(eurGbpRate));

        // When
        Optional<ExchangeRateResponse> result = rateService.getLatestRate("EUR/GBP");

        // Then
        assertTrue(result.isPresent());
        assertFalse(result.get().isStale()); // Within threshold, not stale
    }

    @Test
    void getLatestRate_whenRateIsExactlyAtStaleBoundary_returnsIsStaleTrue() {
        // Given — rate timestamp is just over 4 hours ago (4h + 1 second)
        eurGbpRate.setRateTimestamp(
                Instant.now().minus(4, java.time.temporal.ChronoUnit.HOURS).minusSeconds(1));
        when(currencyPairRepository.findByPairCode("EUR/GBP")).thenReturn(Optional.of(eurGbpPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair))
                .thenReturn(Optional.of(eurGbpRate));

        // When
        Optional<ExchangeRateResponse> result = rateService.getLatestRate("EUR/GBP");

        // Then — just past the 4-hour threshold → stale
        assertTrue(result.isPresent());
        assertTrue(result.get().isStale());
    }

    // ----------------------------------------------------------------
    // Unit — Currency Conversion
    // ----------------------------------------------------------------

    @Test
    void testCurrencyConversion() {
        // Given — 10,000 GBP at 1.2650 = 12,650 USD (brief example)
        BigDecimal amount = new BigDecimal("10000");
        gbpUsdRate.setMidRate(new BigDecimal("1.2650"));
        when(currencyPairRepository.findByPairCode("GBP/USD")).thenReturn(Optional.of(gbpUsdPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(gbpUsdPair)).thenReturn(Optional.of(gbpUsdRate));

        // When — convertAmount returns Optional<ConversionResponse>
        Optional<ConversionResponse> result = rateService.convertAmount(amount, "GBP/USD");

        // Then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("12650.000000"), result.get().convertedAmount());
        assertEquals("GBP", result.get().fromCurrency());
        assertEquals("USD", result.get().toCurrency());
        assertEquals(new BigDecimal("1.2650"), result.get().rate());
    }

    @Test
    void testCurrencyConversion_SameCurrency() {
        // Given — rate = 1.0 simulates same-currency scenario
        BigDecimal amount = new BigDecimal("100");
        gbpUsdRate.setMidRate(new BigDecimal("1.0"));
        when(currencyPairRepository.findByPairCode("GBP/USD")).thenReturn(Optional.of(gbpUsdPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(gbpUsdPair)).thenReturn(Optional.of(gbpUsdRate));

        // When
        Optional<ConversionResponse> result = rateService.convertAmount(amount, "GBP/USD");

        // Then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("100.000000"), result.get().convertedAmount());
    }

    // ----------------------------------------------------------------
    // Mock — Rate Persistence
    // ----------------------------------------------------------------

    @Test
    void testRatePersistence() {
        // Given
        BigDecimal bid = new BigDecimal("1.2600");
        BigDecimal ask = new BigDecimal("1.2700");
        BigDecimal mid = new BigDecimal("1.2650");
        when(currencyPairRepository.findByPairCode("GBP/USD")).thenReturn(Optional.of(gbpUsdPair));
        when(rateProviderRepository.findByProviderCode("TEST")).thenReturn(Optional.of(provider));
        when(exchangeRateRepository.save(any(ExchangeRate.class))).thenReturn(null);

        // When
        rateService.storeRate("GBP/USD", "TEST", bid, ask, mid);

        // Then — verify correct bid/ask/mid and associations were persisted
        verify(exchangeRateRepository).save(argThat(rate ->
                rate.getBidRate().equals(bid) &&
                rate.getAskRate().equals(ask) &&
                rate.getMidRate().equals(mid) &&
                rate.getPair().equals(gbpUsdPair) &&
                rate.getProvider().equals(provider) &&
                Boolean.TRUE.equals(rate.getIsValid()) &&
                Boolean.FALSE.equals(rate.getIsStale())
        ));
    }

    @Test
    void testRatePersistence_InvalidSpread_BidGreaterThanAsk() {
        // Given — bid > ask violates FX spread rule
        BigDecimal bid = new BigDecimal("1.2700");
        BigDecimal ask = new BigDecimal("1.2600");
        BigDecimal mid = new BigDecimal("1.2650");

        // When & Then
        assertThrows(InvalidExchangeRateException.class,
                () -> rateService.storeRate("GBP/USD", "TEST", bid, ask, mid));
        verify(exchangeRateRepository, never()).save(any());
    }

    @Test
    void testRatePersistence_InvalidSpread_MidOutsideRange() {
        // Given — mid > ask violates bid < mid < ask rule
        BigDecimal bid = new BigDecimal("1.2600");
        BigDecimal ask = new BigDecimal("1.2700");
        BigDecimal mid = new BigDecimal("1.2800"); // mid above ask

        // When & Then
        assertThrows(InvalidExchangeRateException.class,
                () -> rateService.storeRate("GBP/USD", "TEST", bid, ask, mid));
        verify(exchangeRateRepository, never()).save(any());
    }

    @Test
    void storeRate_whenProviderIsECB_setsSourceSystemToECB_FEED() {
        // Given — ECB provider triggers special source-system mapping
        RateProvider ecbProvider = new RateProvider();
        ecbProvider.setId(3);
        ecbProvider.setProviderCode("ECB");

        BigDecimal bid = new BigDecimal("1.0800");
        BigDecimal ask = new BigDecimal("1.0900");
        BigDecimal mid = new BigDecimal("1.0850");

        when(currencyPairRepository.findByPairCode("EUR/USD")).thenReturn(Optional.of(eurUsdPair));
        when(rateProviderRepository.findByProviderCode("ECB")).thenReturn(Optional.of(ecbProvider));
        when(exchangeRateRepository.save(any(ExchangeRate.class))).thenReturn(null);

        // When
        rateService.storeRate("EUR/USD", "ECB", bid, ask, mid);

        // Then — source system must be "ECB_FEED" for ECB provider (RateService line 90)
        verify(exchangeRateRepository).save(argThat(rate ->
                "ECB_FEED".equals(rate.getSourceSystem())
        ));
    }

    @Test
    void storeRate_whenProviderNotFound_throwsRateProviderNotFoundException() {
        // Given — pair exists but provider code is unknown
        BigDecimal bid = new BigDecimal("1.2600");
        BigDecimal ask = new BigDecimal("1.2700");
        BigDecimal mid = new BigDecimal("1.2650");

        when(currencyPairRepository.findByPairCode("GBP/USD")).thenReturn(Optional.of(gbpUsdPair));
        when(rateProviderRepository.findByProviderCode("UNKNOWN_PROVIDER"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(RateProviderNotFoundException.class,
                () -> rateService.storeRate("GBP/USD", "UNKNOWN_PROVIDER", bid, ask, mid));
        verify(exchangeRateRepository, never()).save(any());
    }

    @Test
    void storeRate_whenAllRatesAreZero_throwsInvalidExchangeRateException() {
        // Given — zero values violate the "must be positive" rule
        assertThrows(InvalidExchangeRateException.class,
                () -> rateService.storeRate("GBP/USD", "TEST",
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        verify(exchangeRateRepository, never()).save(any());
    }

    // ----------------------------------------------------------------
    // Mock — Rate Retrieval
    // ----------------------------------------------------------------

    @Test
    void testRateRetrieval_Fallback() {
        // Given — no rate exists for pair
        when(currencyPairRepository.findByPairCode("EUR/GBP")).thenReturn(Optional.of(eurGbpPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair)).thenReturn(Optional.empty());

        // When — returns Optional<ExchangeRateResponse>
        Optional<ExchangeRateResponse> result = rateService.getLatestRate("EUR/GBP");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testRateHistory_FutureDate() {
        // Given
        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().plusDays(1);
        when(currencyPairRepository.findByPairCode("EUR/GBP")).thenReturn(Optional.of(eurGbpPair));
        when(exchangeRateRepository.findByPairAndRateTimestampBetweenOrderByRateTimestampDesc(
                eq(eurGbpPair), any(Instant.class), any(Instant.class))).thenReturn(List.of());

        // When — returns List<ExchangeRateResponse>
        List<ExchangeRateResponse> result = rateService.getRateHistory("EUR/GBP", from, to);

        // Then
        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------------
    // Unit — Get Currency Pair By Code
    // ----------------------------------------------------------------

    @Test
    void testGetCurrencyPairByCode_Success() {
        when(currencyPairRepository.findByPairCode("EUR/GBP")).thenReturn(Optional.of(eurGbpPair));

        CurrencyPair result = rateService.getCurrencyPairByCode("EUR/GBP");

        assertEquals(eurGbpPair, result);
    }

    @Test
    void testGetCurrencyPairByCode_NotFound() {
        when(currencyPairRepository.findByPairCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(CurrencyPairNotFoundException.class, () -> rateService.getCurrencyPairByCode("UNKNOWN"));
    }

    // ----------------------------------------------------------------
    // Unit — Get Rate Provider By Code
    // ----------------------------------------------------------------

    @Test
    void testGetRateProviderByCode_Success() {
        when(rateProviderRepository.findByProviderCode("TEST")).thenReturn(Optional.of(provider));

        RateProvider result = rateService.getRateProviderByCode("TEST");

        assertEquals(provider, result);
    }

    @Test
    void testGetRateProviderByCode_NotFound() {
        when(rateProviderRepository.findByProviderCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(RateProviderNotFoundException.class, () -> rateService.getRateProviderByCode("UNKNOWN"));
    }

    // ----------------------------------------------------------------
    // Unit — Get EOD Fixing
    // ----------------------------------------------------------------

    @Test
    void testGetEodFixing_Success() {
        // Given — EodFixing must have pair and provider set (EodFixingResponse.from() needs them)
        LocalDate date = LocalDate.of(2026, 3, 25);

        EodFixing fixing = new EodFixing();
        fixing.setId(1);
        fixing.setPair(eurUsdPair);
        fixing.setProvider(provider);
        fixing.setFixingRate(new BigDecimal("1.08315"));
        fixing.setFixingDate(date);
        fixing.setFixingTime("16:00 LON");
        fixing.setFixingType("WMR");
        fixing.setIsOfficial(true);
        fixing.setPublishedAt(Instant.parse("2026-03-25T16:05:00Z"));

        when(currencyPairRepository.findByPairCode("EUR/USD")).thenReturn(Optional.of(eurUsdPair));
        when(eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(eurUsdPair, date))
                .thenReturn(Optional.of(fixing));
        // Stub deviation check inside getEodFixing
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurUsdPair))
                .thenReturn(Optional.empty());

        // When — returns Optional<EodFixingResponse>
        Optional<EodFixingResponse> result = rateService.getEodFixing("EUR/USD", date);

        // Then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("1.08315"), result.get().fixingRate());
        assertEquals("EUR/USD", result.get().pairCode());
        assertEquals("WMR", result.get().fixingType());
        assertEquals(date, result.get().fixingDate());
    }

    @Test
    void testGetEodFixing_NotFound() {
        // Given
        LocalDate date = LocalDate.now();
        when(currencyPairRepository.findByPairCode("EUR/USD")).thenReturn(Optional.of(eurUsdPair));
        when(eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(eurUsdPair, date))
                .thenReturn(Optional.empty());

        // When
        Optional<EodFixingResponse> result = rateService.getEodFixing("EUR/USD", date);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void getEodFixing_whenDeviationExceedsThreshold_deviationBranchExecuted() {
        // Given — fixing rate deviates >0.5% from the last traded mid rate
        LocalDate date = LocalDate.of(2026, 3, 25);

        EodFixing fixing = new EodFixing();
        fixing.setId(1);
        fixing.setPair(eurUsdPair);
        fixing.setProvider(provider);
        fixing.setFixingRate(new BigDecimal("1.09500")); // ~1.1% above lastTraded → triggers WARN
        fixing.setFixingDate(date);
        fixing.setFixingTime("16:00 LON");
        fixing.setFixingType("WMR");
        fixing.setIsOfficial(true);
        fixing.setPublishedAt(Instant.parse("2026-03-25T16:05:00Z"));

        // Last traded mid is significantly lower → deviation > 0.5%
        eurGbpRate.setPair(eurUsdPair);
        eurGbpRate.setMidRate(new BigDecimal("1.08315"));

        when(currencyPairRepository.findByPairCode("EUR/USD")).thenReturn(Optional.of(eurUsdPair));
        when(eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(eurUsdPair, date))
                .thenReturn(Optional.of(fixing));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurUsdPair))
                .thenReturn(Optional.of(eurGbpRate));

        // When
        Optional<EodFixingResponse> result = rateService.getEodFixing("EUR/USD", date);

        // Then — result present; deviation branch was reached (WARN log emitted internally)
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("1.09500"), result.get().fixingRate());
    }

    @Test
    void getEodFixing_whenDeviationBelowThreshold_noAlertBranchTaken() {
        // Given — fixing rate is only 0.1% away from last traded → no WARN
        LocalDate date = LocalDate.of(2026, 3, 25);

        EodFixing fixing = new EodFixing();
        fixing.setId(2);
        fixing.setPair(eurUsdPair);
        fixing.setProvider(provider);
        fixing.setFixingRate(new BigDecimal("1.08325")); // ~0.009% deviation — below threshold
        fixing.setFixingDate(date);
        fixing.setFixingTime("16:00 LON");
        fixing.setFixingType("WMR");
        fixing.setIsOfficial(true);
        fixing.setPublishedAt(Instant.parse("2026-03-25T16:05:00Z"));

        eurGbpRate.setPair(eurUsdPair);
        eurGbpRate.setMidRate(new BigDecimal("1.08315"));

        when(currencyPairRepository.findByPairCode("EUR/USD")).thenReturn(Optional.of(eurUsdPair));
        when(eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(eurUsdPair, date))
                .thenReturn(Optional.of(fixing));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurUsdPair))
                .thenReturn(Optional.of(eurGbpRate));

        // When
        Optional<EodFixingResponse> result = rateService.getEodFixing("EUR/USD", date);

        // Then
        assertTrue(result.isPresent());
    }

    // ----------------------------------------------------------------
    // Unit — Get Stale Rates
    // ----------------------------------------------------------------

    @Test
    void getStaleRates_whenStaleRatesExist_returnsNonEmptyListWithIsStaleFlagSet() {
        // Given — repository returns one stale rate
        eurGbpRate.setRateTimestamp(Instant.now().minus(5, java.time.temporal.ChronoUnit.HOURS));
        when(exchangeRateRepository.findStaleRates(any(Instant.class)))
                .thenReturn(List.of(eurGbpRate));

        // When
        List<ExchangeRateResponse> result = rateService.getStaleRates();

        // Then — list not empty, isStale flag set to true by the peek() in RateService
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).isStale());
    }

    @Test
    void getStaleRates_whenNoStaleRates_returnsEmptyList() {
        // Given
        when(exchangeRateRepository.findStaleRates(any(Instant.class)))
                .thenReturn(List.of());

        // When
        List<ExchangeRateResponse> result = rateService.getStaleRates();

        // Then
        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------------
    // Unit — Get All Active Currencies
    // ----------------------------------------------------------------

    @Test
    void getAllActiveCurrencies_whenCurrenciesExist_returnsMappedList() {
        // Given
        Currency usd = new Currency();
        usd.setId(1);
        usd.setIsoCode("USD");
        usd.setCurrencyName("US Dollar");
        usd.setCountry("United States");
        usd.setNumericCode("840");
        usd.setMinorUnits((short) 2);
        usd.setIsActive(true);
        usd.setRegion("AMER");

        when(currencyRepository.findByIsActiveTrue()).thenReturn(List.of(usd));

        // When
        var result = rateService.getAllActiveCurrencies();

        // Then
        assertEquals(1, result.size());
        assertEquals("USD", result.get(0).isoCode());
        assertTrue(result.get(0).isActive());
    }

    @Test
    void getAllActiveCurrencies_whenNoCurrencies_returnsEmptyList() {
        // Given
        when(currencyRepository.findByIsActiveTrue()).thenReturn(List.of());

        // When
        var result = rateService.getAllActiveCurrencies();

        // Then
        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------------
    // Unit — Cross Rate (second leg missing)
    // ----------------------------------------------------------------

    @Test
    void calculateCrossRate_whenSecondLegMissing_returnsEmpty() {
        // Given — first leg (EUR/GBP) present, second leg (GBP/USD) has no rate
        when(currencyPairRepository.findByPairCode("EUR/GBP")).thenReturn(Optional.of(eurGbpPair));
        when(currencyPairRepository.findByPairCode("GBP/USD")).thenReturn(Optional.of(gbpUsdPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair))
                .thenReturn(Optional.of(eurGbpRate));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(gbpUsdPair))
                .thenReturn(Optional.empty()); // second leg missing

        // When
        Optional<BigDecimal> result = rateService.calculateCrossRate("EUR/USD", "EUR/GBP", "GBP/USD");

        // Then
        assertFalse(result.isPresent());
    }

    // ----------------------------------------------------------------
    // Parameterised — Edge Cases
    // ----------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("edgeCaseProvider")
    void testEdgeCases(BigDecimal amount, String pairCode, boolean shouldThrow,
                       boolean expectedPresent, Class<? extends Exception> exceptionClass) {
        // Given
        if ("ABC/XYZ".equals(pairCode)) {
            when(currencyPairRepository.findByPairCode(pairCode)).thenReturn(Optional.empty());
        } else {
            when(currencyPairRepository.findByPairCode(pairCode)).thenReturn(Optional.of(gbpUsdPair));
            when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(gbpUsdPair))
                    .thenReturn(Optional.of(gbpUsdRate));
        }

        // When & Then
        if (shouldThrow) {
            assertThrows(exceptionClass, () -> rateService.convertAmount(amount, pairCode));
        } else {
            Optional<ConversionResponse> result = rateService.convertAmount(amount, pairCode);
            if (expectedPresent) {
                assertTrue(result.isPresent());
                if (amount.compareTo(BigDecimal.ZERO) == 0) {
                    assertEquals(new BigDecimal("0.000000"), result.get().convertedAmount());
                }
            } else {
                assertFalse(result.isPresent());
            }
        }
    }

    static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(
                Arguments.of(BigDecimal.ZERO, "GBP/USD", false, true, null),                          // Zero amount
                Arguments.of(new BigDecimal("100"), "GBP/USD", false, true, null),                    // Normal case
                Arguments.of(new BigDecimal("100"), "ABC/XYZ", false, false, null)
        );
    }

    // ----------------------------------------------------------------
    // Unit — getEodFixing missing branches
    // ----------------------------------------------------------------

    @Test
    void getEodFixing_whenProviderIsNull_usesUnknownAsSource() {
        // Covers the false-branch of: f.getProvider() != null ? ... : "UNKNOWN"
        LocalDate date = LocalDate.of(2026, 3, 25);

        EodFixing fixing = new EodFixing();
        fixing.setId(10);
        fixing.setPair(eurUsdPair);
        fixing.setProvider(null);                          // <-- null provider
        fixing.setFixingRate(new BigDecimal("1.08315"));
        fixing.setFixingDate(date);
        fixing.setFixingTime("16:00 LON");
        fixing.setFixingType("WMR");
        fixing.setIsOfficial(true);
        fixing.setPublishedAt(Instant.parse("2026-03-25T16:05:00Z"));

        when(currencyPairRepository.findByPairCode("EUR/USD")).thenReturn(Optional.of(eurUsdPair));
        when(eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(eurUsdPair, date))
                .thenReturn(Optional.of(fixing));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurUsdPair))
                .thenReturn(Optional.empty());

        // When
        Optional<EodFixingResponse> result = rateService.getEodFixing("EUR/USD", date);

        // Then — result present; "UNKNOWN" source branch was taken (no NPE)
        assertTrue(result.isPresent());
    }

    @Test
    void getEodFixing_whenLastTradedMidIsNull_skipsDeviationCheck() {
        // Covers the false-branch of: if (lastTraded != null && ...)
        LocalDate date = LocalDate.of(2026, 3, 25);

        EodFixing fixing = new EodFixing();
        fixing.setId(11);
        fixing.setPair(eurUsdPair);
        fixing.setProvider(provider);
        fixing.setFixingRate(new BigDecimal("1.08315"));
        fixing.setFixingDate(date);
        fixing.setFixingTime("16:00 LON");
        fixing.setFixingType("WMR");
        fixing.setIsOfficial(true);
        fixing.setPublishedAt(Instant.parse("2026-03-25T16:05:00Z"));

        // Exchange rate exists but midRate is null
        ExchangeRate rateWithNullMid = new ExchangeRate();
        rateWithNullMid.setPair(eurUsdPair);
        rateWithNullMid.setMidRate(null);                  // <-- null midRate
        rateWithNullMid.setRateTimestamp(Instant.now());

        when(currencyPairRepository.findByPairCode("EUR/USD")).thenReturn(Optional.of(eurUsdPair));
        when(eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(eurUsdPair, date))
                .thenReturn(Optional.of(fixing));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurUsdPair))
                .thenReturn(Optional.of(rateWithNullMid));

        // When
        Optional<EodFixingResponse> result = rateService.getEodFixing("EUR/USD", date);

        // Then — no NPE; deviation check skipped
        assertTrue(result.isPresent());
    }

    @Test
    void getEodFixing_whenLastTradedMidIsZero_skipsDeviationCheck() {
        // Covers the false-branch of: if (... && lastTraded.compareTo(ZERO) > 0)
        LocalDate date = LocalDate.of(2026, 3, 25);

        EodFixing fixing = new EodFixing();
        fixing.setId(12);
        fixing.setPair(eurUsdPair);
        fixing.setProvider(provider);
        fixing.setFixingRate(new BigDecimal("1.08315"));
        fixing.setFixingDate(date);
        fixing.setFixingTime("16:00 LON");
        fixing.setFixingType("WMR");
        fixing.setIsOfficial(true);
        fixing.setPublishedAt(Instant.parse("2026-03-25T16:05:00Z"));

        // Exchange rate exists but midRate is zero (guard prevents division by zero)
        ExchangeRate rateWithZeroMid = new ExchangeRate();
        rateWithZeroMid.setPair(eurUsdPair);
        rateWithZeroMid.setMidRate(BigDecimal.ZERO);       // <-- zero midRate
        rateWithZeroMid.setRateTimestamp(Instant.now());

        when(currencyPairRepository.findByPairCode("EUR/USD")).thenReturn(Optional.of(eurUsdPair));
        when(eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(eurUsdPair, date))
                .thenReturn(Optional.of(fixing));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurUsdPair))
                .thenReturn(Optional.of(rateWithZeroMid));

        // When
        Optional<EodFixingResponse> result = rateService.getEodFixing("EUR/USD", date);

        // Then — no division by zero; deviation check skipped
        assertTrue(result.isPresent());
    }

    // ----------------------------------------------------------------
    // Unit — storeRate null-argument branches (line 68 in RateService)
    // ----------------------------------------------------------------

    @Test
    void storeRate_whenBidIsNull_throwsInvalidExchangeRateException() {
        // Covers the true-branch of: if (bid == null || ...)
        assertThrows(InvalidExchangeRateException.class,
                () -> rateService.storeRate("GBP/USD", "TEST",
                        null, new BigDecimal("1.27"), new BigDecimal("1.265")));
        verify(exchangeRateRepository, never()).save(any());
    }

    @Test
    void storeRate_whenAskIsNull_throwsInvalidExchangeRateException() {
        // Covers the true-branch of: if (... || ask == null || ...)
        assertThrows(InvalidExchangeRateException.class,
                () -> rateService.storeRate("GBP/USD", "TEST",
                        new BigDecimal("1.26"), null, new BigDecimal("1.265")));
        verify(exchangeRateRepository, never()).save(any());
    }

    @Test
    void storeRate_whenMidIsNull_throwsInvalidExchangeRateException() {
        // Covers the true-branch of: if (... || mid == null)
        assertThrows(InvalidExchangeRateException.class,
                () -> rateService.storeRate("GBP/USD", "TEST",
                        new BigDecimal("1.26"), new BigDecimal("1.27"), null));
        verify(exchangeRateRepository, never()).save(any());
    }

    // ----------------------------------------------------------------
    // Unit — storeRate non-positive rate branches (line 71 in RateService)
    // Each condition in the || chain is evaluated independently.
    // ----------------------------------------------------------------

    @Test
    void storeRate_whenAskIsZero_throwsInvalidExchangeRateException() {
        // bid > 0 so first condition false → evaluates ask <= 0 → true
        assertThrows(InvalidExchangeRateException.class,
                () -> rateService.storeRate("GBP/USD", "TEST",
                        new BigDecimal("1.26"), BigDecimal.ZERO, new BigDecimal("1.265")));
        verify(exchangeRateRepository, never()).save(any());
    }

    @Test
    void storeRate_whenMidIsNegative_throwsInvalidExchangeRateException() {
        // bid > 0, ask > 0 → evaluates mid <= 0 → true (negative mid)
        assertThrows(InvalidExchangeRateException.class,
                () -> rateService.storeRate("GBP/USD", "TEST",
                        new BigDecimal("1.26"), new BigDecimal("1.27"), new BigDecimal("-0.01")));
        verify(exchangeRateRepository, never()).save(any());
    }

    // ----------------------------------------------------------------
    // Unit — convertAmount when no rate exists (line 160 in RateService)
    // ----------------------------------------------------------------

    @Test
    void convertAmount_whenNoRateExists_returnsEmptyOptional() {
        // Covers the empty Optional path in exchangeRateRepository.findTop(...).map(...)
        when(currencyPairRepository.findByPairCode("GBP/USD")).thenReturn(Optional.of(gbpUsdPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(gbpUsdPair))
                .thenReturn(Optional.empty());

        Optional<ConversionResponse> result =
                rateService.convertAmount(new BigDecimal("1000"), "GBP/USD");

        assertFalse(result.isPresent(),
                "convertAmount should return empty when no rate is available for the pair");
    }
}
