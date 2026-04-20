package com.FXplore.fx_rate_service;

import com.FXplore.fx_rate_service.controller.RateController;
import com.FXplore.fx_rate_service.dto.ConversionResponse;
import com.FXplore.fx_rate_service.dto.CurrencyResponse;
import com.FXplore.fx_rate_service.dto.EodFixingResponse;
import com.FXplore.fx_rate_service.dto.ExchangeRateResponse;
import com.FXplore.fx_rate_service.exception.CurrencyPairNotFoundException;
import com.FXplore.fx_rate_service.exception.GlobalExceptionHandler;
import com.FXplore.fx_rate_service.exception.InvalidExchangeRateException;
import com.FXplore.fx_rate_service.exception.RateProviderNotFoundException;
import com.FXplore.fx_rate_service.service.IRateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RateController.class)
@Import(GlobalExceptionHandler.class)
public class RateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IRateService rateService;

    // ----------------------------------------------------------------
    // Helper fixtures
    // ----------------------------------------------------------------

    private ExchangeRateResponse sampleRate() {
        return new ExchangeRateResponse(
                "GBPUSD",
                "REUTERS",
                new BigDecimal("1.2600"),
                new BigDecimal("1.2700"),
                new BigDecimal("1.2650"),
                Instant.parse("2026-03-26T08:00:00Z"),
                "REUTERS",
                true,
                false
        );
    }

    private EodFixingResponse sampleFixing() {
        return new EodFixingResponse(
                "EURUSD",
                "WMR",
                LocalDate.of(2026, 3, 25),
                new BigDecimal("1.08315"),
                "16:00 LON",
                "WMR",
                true,
                Instant.parse("2026-03-25T16:05:00Z")
        );
    }

    private ConversionResponse sampleConversion() {
        return new ConversionResponse(
                "GBP",
                "USD",
                new BigDecimal("10000"),
                new BigDecimal("12650.000000"),
                new BigDecimal("1.2650"),
                "GBP/USD"
        );
    }

    private CurrencyResponse sampleCurrency() {
        return new CurrencyResponse(1, "GBP", "Pound Sterling", "United Kingdom",
                "826", (short) 2, true, "EMEA");
    }

    // ================================================================
    // POST /api/rates
    // ================================================================

    @Test
    void postRates_ValidRequest_Returns201() throws Exception {
        // Given
        String body = """
                {
                    "pairCode": "GBPUSD",
                    "providerCode": "REUTERS",
                    "bid": "1.2600",
                    "ask": "1.2700",
                    "mid": "1.2650"
                }
                """;
        doNothing().when(rateService).storeRate(
                eq("GBPUSD"), eq("REUTERS"),
                eq(new BigDecimal("1.2600")),
                eq(new BigDecimal("1.2700")),
                eq(new BigDecimal("1.2650")));

        // When & Then
        mockMvc.perform(post("/api/rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(containsString("GBPUSD")));
    }

    @Test
    void postRates_InvalidSpread_BidGreaterThanAsk_Returns400() throws Exception {
        // Given — bid > ask: FX rule violated
        String body = """
                {
                    "pairCode": "GBPUSD",
                    "providerCode": "REUTERS",
                    "bid": "1.2700",
                    "ask": "1.2600",
                    "mid": "1.2650"
                }
                """;

        // When & Then — @AssertTrue(isSpreadValid) triggers validation failure
        mockMvc.perform(post("/api/rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void postRates_MissingPairCode_Returns400() throws Exception {
        // Given — pairCode absent
        String body = """
                {
                    "providerCode": "REUTERS",
                    "bid": "1.2600",
                    "ask": "1.2700",
                    "mid": "1.2650"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.pairCode").exists());
    }

    @Test
    void postRates_NegativeBid_Returns400() throws Exception {
        // Given — @Positive constraint violated
        String body = """
                {
                    "pairCode": "GBPUSD",
                    "providerCode": "REUTERS",
                    "bid": "-1.00",
                    "ask": "1.2700",
                    "mid": "1.2650"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.bid").exists());
    }

    @Test
    void postRates_UnknownPair_Returns404() throws Exception {
        // Given — service throws CurrencyPairNotFoundException
        String body = """
                {
                    "pairCode": "XXXXXX",
                    "providerCode": "REUTERS",
                    "bid": "1.2600",
                    "ask": "1.2700",
                    "mid": "1.2650"
                }
                """;
        doThrow(new CurrencyPairNotFoundException("Currency pair not found: XXXXXX"))
                .when(rateService).storeRate(eq("XXXXXX"), any(), any(), any(), any());

        // When & Then
        mockMvc.perform(post("/api/rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("XXXXXX")));
    }

    // ================================================================
    // GET /api/rates?pair=
    // ================================================================

    @Test
    void getRates_KnownPair_Returns200WithRate() throws Exception {
        // Given
        when(rateService.getLatestRate("GBPUSD")).thenReturn(Optional.of(sampleRate()));

        // When & Then
        mockMvc.perform(get("/api/rates").param("pair", "GBPUSD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pairCode").value("GBPUSD"))
                .andExpect(jsonPath("$.midRate").value(1.265))
                .andExpect(jsonPath("$.isStale").value(false));
    }

    @Test
    void getRates_UnknownPair_Returns404() throws Exception {
        // Given
        when(rateService.getLatestRate("XXXXXX")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/rates").param("pair", "XXXXXX"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRates_StaleRate_ReturnsIsStaleTrue() throws Exception {
        // Given — stale flag set in response
        ExchangeRateResponse staleRate = new ExchangeRateResponse(
                "GBPUSD", "REUTERS",
                new BigDecimal("1.2600"), new BigDecimal("1.2700"), new BigDecimal("1.2650"),
                Instant.parse("2026-03-26T04:00:00Z"),
                "REUTERS", true, true   // isStale = true
        );
        when(rateService.getLatestRate("GBPUSD")).thenReturn(Optional.of(staleRate));

        // When & Then
        mockMvc.perform(get("/api/rates").param("pair", "GBPUSD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isStale").value(true));
    }

    // ================================================================
    // GET /api/rates/stale
    // ================================================================

    @Test
    void getStaleRates_Returns200WithList() throws Exception {
        // Given
        when(rateService.getStaleRates()).thenReturn(List.of(sampleRate(), sampleRate()));

        // When & Then
        mockMvc.perform(get("/api/rates/stale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getStaleRates_NoneStale_Returns200WithEmptyList() throws Exception {
        // Given
        when(rateService.getStaleRates()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/rates/stale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ================================================================
    // GET /api/rates/history?pair=&from=&to=
    // ================================================================

    @Test
    void getRateHistory_ValidRange_Returns200WithList() throws Exception {
        // Given
        when(rateService.getRateHistory(
                eq("GBPUSD"),
                eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2026, 3, 26))))
                .thenReturn(List.of(sampleRate()));

        // When & Then
        mockMvc.perform(get("/api/rates/history")
                        .param("pair", "GBPUSD")
                        .param("from", "2026-01-01")
                        .param("to", "2026-03-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pairCode").value("GBPUSD"));
    }

    @Test
    void getRateHistory_FutureRange_Returns200WithEmptyList() throws Exception {
        // Given
        when(rateService.getRateHistory(any(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/rates/history")
                        .param("pair", "GBPUSD")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ================================================================
    // GET /api/convert?from=&to=&amount=
    // ================================================================

    @Test
    void getConvert_ValidRequest_Returns200() throws Exception {
        // Given
        when(rateService.convertAmount(eq(new BigDecimal("10000")), eq("GBP/USD")))
                .thenReturn(Optional.of(sampleConversion()));

        // When & Then
        mockMvc.perform(get("/api/convert")
                        .param("from", "GBP")
                        .param("to", "USD")
                        .param("amount", "10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCurrency").value("GBP"))
                .andExpect(jsonPath("$.toCurrency").value("USD"))
                .andExpect(jsonPath("$.convertedAmount").value(12650.0));
    }

    @Test
    void getConvert_UnknownPair_Returns404() throws Exception {
        // Given
        when(rateService.convertAmount(any(), eq("ABC/XYZ"))).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/convert")
                        .param("from", "ABC")
                        .param("to", "XYZ")
                        .param("amount", "100"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getConvert_ZeroAmount_Returns200() throws Exception {
        // Given — zero amount is technically valid (no @Positive on amount param)
        ConversionResponse zeroConversion = new ConversionResponse(
                "GBP", "USD", BigDecimal.ZERO, new BigDecimal("0.000000"),
                new BigDecimal("1.2650"), "GBP/USD");
        when(rateService.convertAmount(eq(BigDecimal.ZERO), eq("GBP/USD")))
                .thenReturn(Optional.of(zeroConversion));

        // When & Then
        mockMvc.perform(get("/api/convert")
                        .param("from", "GBP")
                        .param("to", "USD")
                        .param("amount", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convertedAmount").value(0.0));
    }

    // ================================================================
    // GET /api/fixings?pair=&date=
    // ================================================================

    @Test
    void getFixings_ValidRequest_Returns200() throws Exception {
        // Given
        when(rateService.getEodFixing(eq("EURUSD"), eq(LocalDate.of(2026, 3, 25))))
                .thenReturn(Optional.of(sampleFixing()));

        // When & Then
        mockMvc.perform(get("/api/fixings")
                        .param("pair", "EURUSD")
                        .param("date", "2026-03-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pairCode").value("EURUSD"))
                .andExpect(jsonPath("$.fixingRate").value(1.08315))
                .andExpect(jsonPath("$.fixingType").value("WMR"))
                .andExpect(jsonPath("$.isOfficial").value(true));
    }

    @Test
    void getFixings_NoFixingForDate_Returns404() throws Exception {
        // Given
        when(rateService.getEodFixing(any(), any(LocalDate.class))).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/fixings")
                        .param("pair", "EURUSD")
                        .param("date", "2025-01-01"))
                .andExpect(status().isNotFound());
    }

    // ================================================================
    // GET /api/currencies
    // ================================================================

    @Test
    void getCurrencies_Returns200WithList() throws Exception {
        // Given
        when(rateService.getAllActiveCurrencies()).thenReturn(List.of(sampleCurrency()));

        // When & Then
        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isoCode").value("GBP"))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void getCurrencies_EmptyList_Returns200() throws Exception {
        // Given
        when(rateService.getAllActiveCurrencies()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ================================================================
    // Error response structure
    // ================================================================

    @Test
    void errorResponse_ContainsTimestampStatusPathMessage() throws Exception {
        // Given — trigger a 404 to verify error body structure
        when(rateService.getLatestRate("XXXXXX"))
                .thenThrow(new CurrencyPairNotFoundException("Currency pair not found: XXXXXX"));

        // When & Then — GlobalExceptionHandler must return structured error body
        mockMvc.perform(get("/api/rates").param("pair", "XXXXXX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("XXXXXX")))
                .andExpect(jsonPath("$.path").value("/api/rates"));
    }

    @Test
    void postRates_whenRateProviderNotFound_returns404() throws Exception {
        // Given — service throws RateProviderNotFoundException (unknown providerCode)
        String body = """
                {
                    "pairCode": "EUR/USD",
                    "providerCode": "UNKNOWN_PROVIDER",
                    "bid": "1.0800",
                    "ask": "1.0900",
                    "mid": "1.0850"
                }
                """;
        doThrow(new RateProviderNotFoundException("Rate provider not found: UNKNOWN_PROVIDER"))
                .when(rateService).storeRate(eq("EUR/USD"), eq("UNKNOWN_PROVIDER"),
                        any(), any(), any());

        // When & Then — handleRateProviderNotFound → 404
        mockMvc.perform(post("/api/rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("UNKNOWN_PROVIDER")));
    }

    @Test
    void postRates_whenInvalidExchangeRate_returns400() throws Exception {
        // Given — service throws InvalidExchangeRateException (e.g. zero rates passed through)
        String body = """
                {
                    "pairCode": "EUR/USD",
                    "providerCode": "REUTERS",
                    "bid": "1.0800",
                    "ask": "1.0900",
                    "mid": "1.0850"
                }
                """;
        doThrow(new InvalidExchangeRateException("Exchange rates must be positive"))
                .when(rateService).storeRate(any(), any(), any(), any(), any());

        // When & Then — handleInvalidExchangeRate → 400
        mockMvc.perform(post("/api/rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("positive")));
    }

    @Test
    void getLatestRate_whenIllegalArgument_returns400() throws Exception {
        // Given — service throws IllegalArgumentException
        when(rateService.getLatestRate("BAD_PAIR"))
                .thenThrow(new IllegalArgumentException("Malformed pair code: BAD_PAIR"));

        // When & Then — handleIllegalArgument → 400
        mockMvc.perform(get("/api/rates").param("pair", "BAD_PAIR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("BAD_PAIR")));
    }

    @Test
    void getLatestRate_whenUnexpectedException_returns500() throws Exception {
        // Given — unexpected runtime exception (e.g. DB connection lost)
        when(rateService.getLatestRate("EUR/USD"))
                .thenThrow(new RuntimeException("Connection pool exhausted"));

        // When & Then — handleGenericException → 500
        mockMvc.perform(get("/api/rates").param("pair", "EUR/USD"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }
}
