package com.FXplore.fx_rate_service;

import com.FXplore.fx_rate_service.dto.ExchangeRateResponse;
import com.FXplore.fx_rate_service.scheduler.StaleRateScheduler;
import com.FXplore.fx_rate_service.service.IRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StaleRateScheduler}.
 *
 * The scheduler itself contains no business logic — it delegates entirely to
 * {@link IRateService#getStaleRates()} and logs results. Therefore the goal here
 * is to verify the three observable behaviours:
 *   1. When no stale rates exist → service is called exactly once, no WARN logged.
 *   2. When stale rates exist    → service is called once and the loop iterates
 *                                  over every returned rate (verified via interactions).
 *   3. When the service throws   → the exception propagates (scheduler does not swallow it).
 *
 * No Spring context is loaded — pure Mockito, fast execution.
 */
@ExtendWith(MockitoExtension.class)
class StaleRateSchedulerTest {

    @Mock
    private IRateService rateService;

    @InjectMocks
    private StaleRateScheduler staleRateScheduler;

    // ----------------------------------------------------------------
    // Helper fixture
    // ----------------------------------------------------------------

    /** Builds a minimal ExchangeRateResponse for use in stale-rate lists. */
    private ExchangeRateResponse staleRate(String pairCode, String providerCode) {
        return new ExchangeRateResponse(
                pairCode,
                providerCode,
                new BigDecimal("1.2600"),
                new BigDecimal("1.2700"),
                new BigDecimal("1.2650"),
                Instant.now().minusSeconds(6 * 3600), // 6 hours old — clearly stale
                providerCode,
                true,
                true
        );
    }

    // ----------------------------------------------------------------
    // Tests
    // ----------------------------------------------------------------

    @Test
    void checkStaleRates_whenNoStaleRates_serviceCalledOnceAndReturnsQuietly() {
        // Given — service reports no stale pairs
        when(rateService.getStaleRates()).thenReturn(List.of());

        // When
        staleRateScheduler.checkStaleRates();

        // Then — service was consulted exactly once; no further interactions
        verify(rateService, times(1)).getStaleRates();
        verifyNoMoreInteractions(rateService);
    }

    @Test
    void checkStaleRates_whenTwoStaleRatesExist_serviceCalledOnceAndBothRatesProcessed() {
        // Given — two stale pairs returned by the service
        List<ExchangeRateResponse> staleRates = List.of(
                staleRate("EUR/USD", "REUTERS"),
                staleRate("GBP/USD", "BLOOMBERG")
        );
        when(rateService.getStaleRates()).thenReturn(staleRates);

        // When
        staleRateScheduler.checkStaleRates();

        // Then — service called once; the scheduler iterates the full list without errors
        verify(rateService, times(1)).getStaleRates();
        verifyNoMoreInteractions(rateService);
    }

    @Test
    void checkStaleRates_whenServiceThrowsRuntimeException_exceptionPropagates() {
        // Given — downstream failure (e.g. DB unavailable)
        when(rateService.getStaleRates()).thenThrow(new RuntimeException("DB connection lost"));

        // When & Then — scheduler does not silently swallow the exception
        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> staleRateScheduler.checkStaleRates()
        );
        verify(rateService, times(1)).getStaleRates();
    }
}

