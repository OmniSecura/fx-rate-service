package com.FXplore.fx_rate_service;

import com.FXplore.fx_rate_service.dao.ICurrencyPairRepository;
import com.FXplore.fx_rate_service.dao.IEodFixingRepository;
import com.FXplore.fx_rate_service.dao.IExchangeRateRepository;
import com.FXplore.fx_rate_service.dao.IRateProviderRepository;
import com.FXplore.fx_rate_service.exception.CurrencyPairNotFoundException;
import com.FXplore.fx_rate_service.exception.InvalidExchangeRateException;
import com.FXplore.fx_rate_service.exception.RateProviderNotFoundException;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
        // Set up test data
        eurGbpPair = new CurrencyPair();
        eurGbpPair.setId(1);
        eurGbpPair.setPairCode("EURGBP");

        gbpUsdPair = new CurrencyPair();
        gbpUsdPair.setId(2);
        gbpUsdPair.setPairCode("GBPUSD");

        eurUsdPair = new CurrencyPair();
        eurUsdPair.setId(3);
        eurUsdPair.setPairCode("EURUSD");

        provider = new RateProvider();
        provider.setId(1);
        provider.setProviderCode("TEST");

        eurGbpRate = new ExchangeRate();
        eurGbpRate.setMidRate(new BigDecimal("0.85"));
        eurGbpRate.setRateTimestamp(Instant.now());

        gbpUsdRate = new ExchangeRate();
        gbpUsdRate.setMidRate(new BigDecimal("1.265"));
        gbpUsdRate.setRateTimestamp(Instant.now());
    }

    // Unit — Cross Rate Calculation
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

    // Unit — Staleness Detection
    @Test
    void testStalenessDetection_Stale() {
        // Given
        Instant staleTime = Instant.now().minus(5, java.time.temporal.ChronoUnit.HOURS);
        eurGbpRate.setRateTimestamp(staleTime);
        when(currencyPairRepository.findByPairCode("EURGBP")).thenReturn(Optional.of(eurGbpPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair)).thenReturn(Optional.of(eurGbpRate));

        // When
        Optional<ExchangeRate> result = rateService.getLatestRate("EURGBP");

        // Then
        assertTrue(result.isPresent());
        assertTrue(result.get().getIsStale());
    }

    @Test
    void testStalenessDetection_Boundary() {
        // Given
        Instant boundaryTime = Instant.now().minus(3, java.time.temporal.ChronoUnit.HOURS);
        eurGbpRate.setRateTimestamp(boundaryTime);
        when(currencyPairRepository.findByPairCode("EURGBP")).thenReturn(Optional.of(eurGbpPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair)).thenReturn(Optional.of(eurGbpRate));

        // When
        Optional<ExchangeRate> result = rateService.getLatestRate("EURGBP");

        // Then
        assertTrue(result.isPresent());
        assertFalse(result.get().getIsStale()); // Within threshold, not stale
    }

    // Unit — Currency Conversion
    @Test
    void testCurrencyConversion() {
        // Given
        BigDecimal amount = new BigDecimal("10000");
        BigDecimal rate = new BigDecimal("1.2650");
        eurGbpRate.setMidRate(rate);
        when(currencyPairRepository.findByPairCode("GBPUSD")).thenReturn(Optional.of(gbpUsdPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(gbpUsdPair)).thenReturn(Optional.of(eurGbpRate));

        // When
        Optional<BigDecimal> result = rateService.convertAmount(amount, "GBPUSD");

        // Then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("12650.000000"), result.get());
    }

    @Test
    void testCurrencyConversion_SameCurrency() {
        // Given
        BigDecimal amount = new BigDecimal("100");
        BigDecimal rate = new BigDecimal("1.0");
        eurGbpRate.setMidRate(rate);
        when(currencyPairRepository.findByPairCode("GBPUSD")).thenReturn(Optional.of(gbpUsdPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(gbpUsdPair)).thenReturn(Optional.of(eurGbpRate));

        // When
        Optional<BigDecimal> result = rateService.convertAmount(amount, "GBPUSD");

        // Then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("100.000000"), result.get());
    }

    // Mock — Rate Persistence
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

        // Then
        verify(exchangeRateRepository).save(argThat(rate -> 
            rate.getBidRate().equals(bid) &&
            rate.getAskRate().equals(ask) &&
            rate.getMidRate().equals(mid) &&
            rate.getPair().equals(gbpUsdPair) &&
            rate.getProvider().equals(provider)
        ));
    }

    // Mock — Rate Retrieval
    @Test
    void testRateRetrieval_Fallback() {
        // Given
        when(currencyPairRepository.findByPairCode("EURGBP")).thenReturn(Optional.of(eurGbpPair));
        when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(eurGbpPair)).thenReturn(Optional.empty());

        // When
        Optional<ExchangeRate> result = rateService.getLatestRate("EURGBP");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testRateHistory_FutureDate() {
        // Given
        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().plusDays(1); // Future date
        when(currencyPairRepository.findByPairCode("EURGBP")).thenReturn(Optional.of(eurGbpPair));
        when(exchangeRateRepository.findByPairAndRateTimestampBetweenOrderByRateTimestampDesc(eq(eurGbpPair), any(Instant.class), any(Instant.class))).thenReturn(List.of());

        // When
        List<ExchangeRate> result = rateService.getRateHistory("EURGBP", from, to);

        // Then
        assertTrue(result.isEmpty());
    }

    // Unit — Get Currency Pair By Code
    @Test
    void testGetCurrencyPairByCode_Success() {
        // Given
        when(currencyPairRepository.findByPairCode("EURGBP")).thenReturn(Optional.of(eurGbpPair));

        // When
        CurrencyPair result = rateService.getCurrencyPairByCode("EURGBP");

        // Then
        assertEquals(eurGbpPair, result);
    }

    @Test
    void testGetCurrencyPairByCode_NotFound() {
        // Given
        when(currencyPairRepository.findByPairCode("UNKNOWN")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CurrencyPairNotFoundException.class, () -> rateService.getCurrencyPairByCode("UNKNOWN"));
    }

    // Unit — Get Rate Provider By Code
    @Test
    void testGetRateProviderByCode_Success() {
        // Given
        when(rateProviderRepository.findByProviderCode("TEST")).thenReturn(Optional.of(provider));

        // When
        RateProvider result = rateService.getRateProviderByCode("TEST");

        // Then
        assertEquals(provider, result);
    }

    @Test
    void testGetRateProviderByCode_NotFound() {
        // Given
        when(rateProviderRepository.findByProviderCode("UNKNOWN")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RateProviderNotFoundException.class, () -> rateService.getRateProviderByCode("UNKNOWN"));
    }

    // Unit — Get EOD Fixing
    @Test
    void testGetEodFixing_Success() {
        // Given
        EodFixing fixing = new EodFixing();
        fixing.setId(1);
        fixing.setFixingRate(new BigDecimal("1.08315"));
        LocalDate date = LocalDate.now();
        when(currencyPairRepository.findByPairCode("EURUSD")).thenReturn(Optional.of(eurUsdPair));
        when(eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(eurUsdPair, date)).thenReturn(Optional.of(fixing));

        // When
        Optional<EodFixing> result = rateService.getEodFixing("EURUSD", date);

        // Then
        assertTrue(result.isPresent());
        assertEquals(fixing, result.get());
    }

    @Test
    void testGetEodFixing_NotFound() {
        // Given
        LocalDate date = LocalDate.now();
        when(currencyPairRepository.findByPairCode("EURUSD")).thenReturn(Optional.of(eurUsdPair));
        when(eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(eurUsdPair, date)).thenReturn(Optional.empty());

        // When
        Optional<EodFixing> result = rateService.getEodFixing("EURUSD", date);

        // Then
        assertFalse(result.isPresent());
    }

    // Parameterised — Edge Cases
    @ParameterizedTest
    @MethodSource("edgeCaseProvider")
    void testEdgeCases(BigDecimal amount, String pairCode, boolean shouldThrow, Class<? extends Exception> exceptionClass) {
        // Given
        if ("UNKNOWN".equals(pairCode)) {
            when(currencyPairRepository.findByPairCode(pairCode)).thenReturn(Optional.empty());
        } else {
            when(currencyPairRepository.findByPairCode(pairCode)).thenReturn(Optional.of(gbpUsdPair));
            when(exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(gbpUsdPair)).thenReturn(Optional.of(eurGbpRate));
        }

        // When & Then
        if (shouldThrow) {
            assertThrows(exceptionClass, () -> rateService.convertAmount(amount, pairCode));
        } else {
            Optional<BigDecimal> result = rateService.convertAmount(amount, pairCode);
            if (amount != null && amount.compareTo(BigDecimal.ZERO) == 0) {
                assertTrue(result.isPresent());
                assertEquals(new BigDecimal("0.000000"), result.get());
            }
        }
    }

    static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(
            Arguments.of(BigDecimal.ZERO, "GBPUSD", false, null), // Zero amount
            Arguments.of(new BigDecimal("100"), "GBPUSD", false, null), // Normal case
            Arguments.of(new BigDecimal("100"), "UNKNOWN", true, CurrencyPairNotFoundException.class) // Unknown pair
        );
    }
}
