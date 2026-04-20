package com.FXplore.fx_rate_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the REST API layer using a real MySQL 8.0 database
 * (Testcontainers) and a full Spring Boot context on a random port.
 *
 * Uses {@link TestRestTemplate} to make real HTTP requests — this means:
 *   - The full request/response cycle is exercised (serialisation, validation,
 *     exception handlers, HTTP status codes).
 *   - {@code @Transactional} does NOT roll back writes here (different thread),
 *     so write tests either verify the response only, or clean up after themselves.
 *
 * Seed data (data.sql): 20 currencies, 20 pairs, 20 stale exchange rates (2026-03-26).
 */
class RateControllerIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    // ================================================================
    // GET /api/currencies
    // ================================================================

    @Test
    void getCurrencies_returns200WithTwentyCurrencies() {
        // When
        ResponseEntity<List> response = restTemplate.getForEntity("/api/currencies", List.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(20, response.getBody().size(),
                "data.sql inserts exactly 20 active currencies");
    }

    // ================================================================
    // GET /api/rates?pair=
    // ================================================================

    @Test
    void getLatestRate_withSeedPair_returns200AndIsStaleTrue() {
        // Seed rate for EUR/USD is from 2026-03-26 — stale on 2026-04-20
        ResponseEntity<Map> response =
                restTemplate.getForEntity("/api/rates?pair=EUR/USD", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("EUR/USD", response.getBody().get("pairCode"));
        assertEquals(Boolean.TRUE, response.getBody().get("isStale"));
    }

    @Test
    void getLatestRate_withUnknownPair_returns404WithErrorBody() {
        ResponseEntity<Map> response =
                restTemplate.getForEntity("/api/rates?pair=ZZZ/QQQ", Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        // GlobalExceptionHandler must populate these fields
        assertEquals(404, response.getBody().get("status"));
        assertTrue(response.getBody().containsKey("timestamp"));
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().containsKey("path"));
    }

    // ================================================================
    // GET /api/rates/stale
    // ================================================================

    @Test
    void getStaleRates_withSeedData_returns200WithTwentyStaleRates() {
        ResponseEntity<List> response =
                restTemplate.getForEntity("/api/rates/stale", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(20, response.getBody().size(),
                "All 20 seed rates from 2026-03-26 are stale on 2026-04-20");
    }

    // ================================================================
    // GET /api/rates/history
    // ================================================================

    @Test
    void getRateHistory_withinSeedDataRange_returns200WithRates() {
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/rates/history?pair=EUR/USD&from=2026-03-26&to=2026-03-26",
                List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty(),
                "At least one EUR/USD rate exists on 2026-03-26");
    }

    @Test
    void getRateHistory_withUnknownPair_returns404() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/rates/history?pair=ZZZ/QQQ&from=2026-01-01&to=2026-12-31",
                Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ================================================================
    // GET /api/convert
    // ================================================================

    @Test
    void convertAmount_withSeedData_returns200WithConvertedAmount() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/convert?from=EUR&to=USD&amount=10000",
                Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("EUR", response.getBody().get("fromCurrency"));
        assertEquals("USD", response.getBody().get("toCurrency"));
        assertNotNull(response.getBody().get("convertedAmount"));
    }

    // ================================================================
    // GET /api/fixings
    // ================================================================

    @Test
    void getEodFixing_whenNoSeedData_returns404() {
        // data.sql contains no eod_fixing rows
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/fixings?pair=EUR/USD&date=2026-03-26",
                Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ================================================================
    // POST /api/rates
    // ================================================================

    @Test
    void postRates_withValidRequest_returns201WithMessage() {
        // Given
        String body = """
                {
                    "pairCode": "EUR/USD",
                    "providerCode": "REUTERS",
                    "bid": "1.0800",
                    "ask": "1.0900",
                    "mid": "1.0850"
                }
                """;
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<String> entity =
                new org.springframework.http.HttpEntity<>(body, headers);

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/rates", entity, Map.class);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").toString().contains("EUR/USD"));
    }

    @Test
    void postRates_withInvalidSpread_returns400WithFieldErrors() {
        // bid > ask → @AssertTrue(isSpreadValid) fires → 400
        String body = """
                {
                    "pairCode": "EUR/USD",
                    "providerCode": "REUTERS",
                    "bid": "1.0900",
                    "ask": "1.0800",
                    "mid": "1.0850"
                }
                """;
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<String> entity =
                new org.springframework.http.HttpEntity<>(body, headers);

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/rates", entity, Map.class);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("fieldErrors"),
                "Validation error response must contain fieldErrors map");
    }

    @Test
    void postRates_withUnknownProvider_returns404() {
        String body = """
                {
                    "pairCode": "EUR/USD",
                    "providerCode": "NO_SUCH_PROVIDER",
                    "bid": "1.0800",
                    "ask": "1.0900",
                    "mid": "1.0850"
                }
                """;
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<String> entity =
                new org.springframework.http.HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/rates", entity, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

