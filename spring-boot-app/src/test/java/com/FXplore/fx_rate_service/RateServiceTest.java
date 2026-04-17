package com.FXplore.fx_rate_service;

import com.FXplore.fx_rate_service.dao.ICurrencyPairRepository;
import com.FXplore.fx_rate_service.dao.IEodFixingRepository;
import com.FXplore.fx_rate_service.dao.IExchangeRateRepository;
import com.FXplore.fx_rate_service.dao.IRateProviderRepository;
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
        eurGbpPair.setPairCode("EURGBP");
        eurGbpPair.setBaseCurrency(eur);
        eurGbpPair.setQuoteCurrency(gbp);

        gbpUsdPair = new CurrencyPair();
        gbpUsdPair.setId(2);
        gbpUsdPair.setPairCode("GBPUSD");
        gbpUsdPair.setBaseCurrency(gbp);
        gbpUsdPair.setQuoteCurrency(usd);

        eurUsdPair = new CurrencyPair();
        eurUsdPair.setId(3);
        eurUsdPair.setPairCode("EURUSD");
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
        when(currencyPairRepository.findByPairCode("EURGBP")).thenReturn(Optional.of(eurGbpPair));
        when(currencyPairRepository.findByPairCode("GBPUSD")).thenReturn(Optional.of(gbpUsdPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair)).thenReturn(Optional.of(eurGbpRate));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(gbpUsdPair)).thenReturn(Optional.of(gbpUsdRate));

        // When
        Optional<BigDecimal> result = rateService.calculateCrossRate("EURUSD", "EURGBP", "GBPUSD");

        // Then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("1.075250"), result.get()); // 0.85 * 1.265 = 1.07525
    }

    @Test
    void testCalculateCrossRate_MissingLeg() {
        // Given
        when(currencyPairRepository.findByPairCode("EURGBP")).thenReturn(Optional.of(eurGbpPair));
        when(currencyPairRepository.findByPairCode("GBPUSD")).thenReturn(Optional.of(gbpUsdPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair)).thenReturn(Optional.empty());

        // When
        Optional<BigDecimal> result = rateService.calculateCrossRate("EURUSD", "EURGBP", "GBPUSD");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testCalculateCrossRate_UnknownPair() {
        // Given
        when(currencyPairRepository.findByPairCode("EURGBP")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CurrencyPairNotFoundException.class,
                () -> rateService.calculateCrossRate("EURUSD", "EURGBP", "GBPUSD"));
    }

    // ----------------------------------------------------------------
    // Unit — Staleness Detection
    // ----------------------------------------------------------------

    @Test
    void testStalenessDetection_Stale() {
        // Given — rate is 5 hours old, threshold is 4 hours
        eurGbpRate.setRateTimestamp(Instant.now().minus(5, java.time.temporal.ChronoUnit.HOURS));
        when(currencyPairRepository.findByPairCode("EURGBP")).thenReturn(Optional.of(eurGbpPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair)).thenReturn(Optional.of(eurGbpRate));

        // When — getLatestRate returns Optional<ExchangeRateResponse> (DTO record)
        Optional<ExchangeRateResponse> result = rateService.getLatestRate("EURGBP");

        // Then
        assertTrue(result.isPresent());
        assertTrue(result.get().isStale()); // record accessor — isStale(), not getIsStale()
    }

    @Test
    void testStalenessDetection_Boundary() {
        // Given — rate is 3 hours old, below the 4-hour threshold
        eurGbpRate.setRateTimestamp(Instant.now().minus(3, java.time.temporal.ChronoUnit.HOURS));
        when(currencyPairRepository.findByPairCode("EURGBP")).thenReturn(Optional.of(eurGbpPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair)).thenReturn(Optional.of(eurGbpRate));

        // When
        Optional<ExchangeRateResponse> result = rateService.getLatestRate("EURGBP");

        // Then
        assertTrue(result.isPresent());
        assertFalse(result.get().isStale()); // Within threshold, not stale
    }

    // ----------------------------------------------------------------
    // Unit — Currency Conversion
    // ----------------------------------------------------------------

    @Test
    void testCurrencyConversion() {
        // Given — 10,000 GBP at 1.2650 = 12,650 USD (brief example)
        BigDecimal amount = new BigDecimal("10000");
        gbpUsdRate.setMidRate(new BigDecimal("1.2650"));
        when(currencyPairRepository.findByPairCode("GBPUSD")).thenReturn(Optional.of(gbpUsdPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(gbpUsdPair)).thenReturn(Optional.of(gbpUsdRate));

        // When — convertAmount returns Optional<ConversionResponse>
        Optional<ConversionResponse> result = rateService.convertAmount(amount, "GBPUSD");

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
        when(currencyPairRepository.findByPairCode("GBPUSD")).thenReturn(Optional.of(gbpUsdPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(gbpUsdPair)).thenReturn(Optional.of(gbpUsdRate));

        // When
        Optional<ConversionResponse> result = rateService.convertAmount(amount, "GBPUSD");

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
        when(currencyPairRepository.findByPairCode("GBPUSD")).thenReturn(Optional.of(gbpUsdPair));
        when(rateProviderRepository.findByProviderCode("TEST")).thenReturn(Optional.of(provider));
        when(exchangeRateRepository.save(any(ExchangeRate.class))).thenReturn(null);

        // When
        rateService.storeRate("GBPUSD", "TEST", bid, ask, mid);

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
                () -> rateService.storeRate("GBPUSD", "TEST", bid, ask, mid));
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
                () -> rateService.storeRate("GBPUSD", "TEST", bid, ask, mid));
        verify(exchangeRateRepository, never()).save(any());
    }

    // ----------------------------------------------------------------
    // Mock — Rate Retrieval
    // ----------------------------------------------------------------

    @Test
    void testRateRetrieval_Fallback() {
        // Given — no rate exists for pair
        when(currencyPairRepository.findByPairCode("EURGBP")).thenReturn(Optional.of(eurGbpPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair)).thenReturn(Optional.empty());

        // When — returns Optional<ExchangeRateResponse>
        Optional<ExchangeRateResponse> result = rateService.getLatestRate("EURGBP");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testRateHistory_FutureDate() {
        // Given
        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().plusDays(1);
        when(currencyPairRepository.findByPairCode("EURGBP")).thenReturn(Optional.of(eurGbpPair));
        when(exchangeRateRepository.findByPairAndRateTimestampBetweenOrderByRateTimestampDesc(
                eq(eurGbpPair), any(Instant.class), any(Instant.class))).thenReturn(List.of());

        // When — returns List<ExchangeRateResponse>
        List<ExchangeRateResponse> result = rateService.getRateHistory("EURGBP", from, to);

        // Then
        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------------
    // Unit — Get Currency Pair By Code
    // ----------------------------------------------------------------

    @Test
    void testGetCurrencyPairByCode_Success() {
        when(currencyPairRepository.findByPairCode("EURGBP")).thenReturn(Optional.of(eurGbpPair));

        CurrencyPair result = rateService.getCurrencyPairByCode("EURGBP");

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

        when(currencyPairRepository.findByPairCode("EURUSD")).thenReturn(Optional.of(eurUsdPair));
        when(eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(eurUsdPair, date))
                .thenReturn(Optional.of(fixing));
        // Stub deviation check inside getEodFixing
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurUsdPair))
                .thenReturn(Optional.empty());

        // When — returns Optional<EodFixingResponse>
        Optional<EodFixingResponse> result = rateService.getEodFixing("EURUSD", date);

        // Then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("1.08315"), result.get().fixingRate());
        assertEquals("EURUSD", result.get().pairCode());
        assertEquals("WMR", result.get().fixingType());
        assertEquals(date, result.get().fixingDate());
    }

    @Test
    void testGetEodFixing_NotFound() {
        // Given
        LocalDate date = LocalDate.now();
        when(currencyPairRepository.findByPairCode("EURUSD")).thenReturn(Optional.of(eurUsdPair));
        when(eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(eurUsdPair, date))
                .thenReturn(Optional.empty());

        // When
        Optional<EodFixingResponse> result = rateService.getEodFixing("EURUSD", date);

        // Then
        assertFalse(result.isPresent());
    }

    // ----------------------------------------------------------------
    // Parameterised — Edge Cases
    // ----------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("edgeCaseProvider")
    void testEdgeCases(BigDecimal amount, String pairCode, boolean shouldThrow,
                       Class<? extends Exception> exceptionClass) {
        // Given
        if ("UNKNOWN".equals(pairCode)) {
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
            // convertAmount returns Optional<ConversionResponse>
            Optional<ConversionResponse> result = rateService.convertAmount(amount, pairCode);
            assertTrue(result.isPresent());
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                assertEquals(new BigDecimal("0.000000"), result.get().convertedAmount());
            }
        }
    }

    static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(
                Arguments.of(BigDecimal.ZERO, "GBPUSD", false, null),                          // Zero amount
                Arguments.of(new BigDecimal("100"), "GBPUSD", false, null),                    // Normal case
                Arguments.of(new BigDecimal("100"), "UNKNOWN", true,                           // Unknown pair
                        CurrencyPairNotFoundException.class)
        );
    }
}
