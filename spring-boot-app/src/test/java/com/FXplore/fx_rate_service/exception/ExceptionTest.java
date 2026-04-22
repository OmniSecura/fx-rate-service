package com.FXplore.fx_rate_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for exception classes and GlobalExceptionHandler.
 *
 * Covers the (message, cause) constructors on all three domain exceptions
 * and the handler methods in GlobalExceptionHandler that are not exercised
 * by the controller-layer integration tests.
 */
class ExceptionTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        // Mock the HttpServletRequest — only getRequestURI() is needed by the handlers
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    // ----------------------------------------------------------------
    // Exception constructors — (message, cause) branch
    // ----------------------------------------------------------------

    @Test
    void currencyPairNotFoundException_withCause_preservesMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        CurrencyPairNotFoundException ex =
                new CurrencyPairNotFoundException("pair not found", cause);

        assertEquals("pair not found", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void invalidExchangeRateException_withCause_preservesMessageAndCause() {
        Throwable cause = new IllegalStateException("original");
        InvalidExchangeRateException ex =
                new InvalidExchangeRateException("invalid rate", cause);

        assertEquals("invalid rate", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void rateProviderNotFoundException_withCause_preservesMessageAndCause() {
        Throwable cause = new RuntimeException("upstream");
        RateProviderNotFoundException ex =
                new RateProviderNotFoundException("provider not found", cause);

        assertEquals("provider not found", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    // ----------------------------------------------------------------
    // GlobalExceptionHandler — handlers not covered by IT tests
    // ----------------------------------------------------------------

    @Test
    void handleConstraintViolation_returns400WithMessage() {
        ConstraintViolationException ex =
                new ConstraintViolationException("constraint violated", Set.of());

        ResponseEntity<Map<String, Object>> response =
                handler.handleConstraintViolation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("constraint violated", response.getBody().get("message"));
        assertEquals("/api/test", response.getBody().get("path"));
    }

    @Test
    void handleIllegalArgument_returns400WithMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("bad argument");

        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgument(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("bad argument", response.getBody().get("message"));
    }

    @Test
    void handleGenericException_returns500WithGenericMessage() {
        Exception ex = new RuntimeException("something went wrong");

        ResponseEntity<Map<String, Object>> response =
                handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Unexpected server error", response.getBody().get("message"));
    }

    @Test
    void handleCurrencyPairNotFound_returns404WithMessage() {
        CurrencyPairNotFoundException ex =
                new CurrencyPairNotFoundException("Currency pair not found: XXX/YYY");

        ResponseEntity<Map<String, Object>> response =
                handler.handleCurrencyPairNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertTrue(response.getBody().containsKey("timestamp"));
        assertEquals("/api/test", response.getBody().get("path"));
    }

    @Test
    void handleRateProviderNotFound_returns404WithMessage() {
        RateProviderNotFoundException ex =
                new RateProviderNotFoundException("Rate provider not found: UNKNOWN");

        ResponseEntity<Map<String, Object>> response =
                handler.handleRateProviderNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
    }

    @Test
    void handleInvalidExchangeRate_returns400WithMessage() {
        InvalidExchangeRateException ex =
                new InvalidExchangeRateException("Bid/ask spread invalid");

        ResponseEntity<Map<String, Object>> response =
                handler.handleInvalidExchangeRate(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bid/ask spread invalid", response.getBody().get("message"));
    }
}

