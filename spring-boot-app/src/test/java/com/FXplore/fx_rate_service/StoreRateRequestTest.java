package com.FXplore.fx_rate_service;

import com.FXplore.fx_rate_service.dto.StoreRateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StoreRateRequest}.
 *
 * Covers two distinct concerns:
 *   1. {@code isValidSpread()} static method — pure logic, no Spring context needed.
 *   2. Bean Validation constraints (@NotNull, @Positive, @AssertTrue) triggered via
 *      the standard Jakarta {@link Validator}, matching what Spring Boot does at
 *      runtime when @Valid is present on the controller method.
 *
 * Using @ParameterizedTest to cover multiple bid/mid/ask combinations
 * efficiently, as recommended in the project marking scheme.
 */
class StoreRateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        // Build a standard Jakarta validator — same engine Spring Boot uses internally
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ================================================================
    // isValidSpread() — static helper
    // ================================================================

    /**
     * Parametrised cases for {@link StoreRateRequest#isValidSpread}.
     * Format: bid, mid, ask, expectedResult, scenarioDescription
     */
    static Stream<Arguments> spreadCases() {
        return Stream.of(
                // Valid: strict bid < mid < ask
                Arguments.of("1.2600", "1.2650", "1.2700", true,  "valid spread bid<mid<ask"),
                // Invalid: bid equals mid
                Arguments.of("1.2650", "1.2650", "1.2700", false, "bid equals mid"),
                // Invalid: mid equals ask
                Arguments.of("1.2600", "1.2700", "1.2700", false, "mid equals ask"),
                // Invalid: bid > ask (completely inverted)
                Arguments.of("1.2700", "1.2650", "1.2600", false, "bid > ask"),
                // Invalid: mid above ask
                Arguments.of("1.2600", "1.2800", "1.2700", false, "mid above ask"),
                // Invalid: bid above mid and ask
                Arguments.of("1.2900", "1.2650", "1.2700", false, "bid above mid and ask"),
                // Edge: very small spread (1 pip) — still valid
                Arguments.of("1.26490", "1.26495", "1.26500", true, "1-pip spread is valid"),
                // Edge: large rate values (JPY pairs)
                Arguments.of("149.870", "149.875", "149.880", true, "large rate values valid"),
                // Null values — null-safety: method must return true (nulls handled by @NotNull)
                Arguments.of(null, null, null, true, "all null returns true (null-safe)")
        );
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("spreadCases")
    void isValidSpread_variousCombinations_returnsExpectedResult(
            String bidStr, String midStr, String askStr,
            boolean expected, String scenario) {

        BigDecimal bid = bidStr != null ? new BigDecimal(bidStr) : null;
        BigDecimal mid = midStr != null ? new BigDecimal(midStr) : null;
        BigDecimal ask = askStr != null ? new BigDecimal(askStr) : null;

        assertEquals(expected, StoreRateRequest.isValidSpread(bid, mid, ask),
                "Scenario: " + scenario);
    }

    // ================================================================
    // Bean Validation — @AssertTrue(isSpreadValid)
    // ================================================================

    @Test
    void beanValidation_validRequest_noViolations() {
        // Given — all constraints satisfied
        StoreRateRequest request = new StoreRateRequest(
                "EUR/USD", "REUTERS",
                new BigDecimal("1.2600"),
                new BigDecimal("1.2700"),
                new BigDecimal("1.2650")
        );

        // When
        Set<ConstraintViolation<StoreRateRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Expected no violations for a valid request");
    }

    @Test
    void beanValidation_invalidSpread_spreadValidViolationRaised() {
        // Given — bid > ask violates @AssertTrue on isSpreadValid()
        StoreRateRequest request = new StoreRateRequest(
                "EUR/USD", "REUTERS",
                new BigDecimal("1.2700"), // bid > ask — invalid
                new BigDecimal("1.2600"),
                new BigDecimal("1.2650")
        );

        // When
        Set<ConstraintViolation<StoreRateRequest>> violations = validator.validate(request);

        // Then — exactly one violation on the spreadValid property
        assertEquals(1, violations.size());
        ConstraintViolation<StoreRateRequest> v = violations.iterator().next();
        assertEquals("spreadValid", v.getPropertyPath().toString());
        assertTrue(v.getMessage().contains("bid < mid < ask"));
    }

    @Test
    void beanValidation_nullPairCode_pairCodeViolationRaised() {
        // Given — @NotBlank violated on pairCode
        StoreRateRequest request = new StoreRateRequest(
                null, "REUTERS",
                new BigDecimal("1.2600"),
                new BigDecimal("1.2700"),
                new BigDecimal("1.2650")
        );

        // When
        Set<ConstraintViolation<StoreRateRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("pairCode")));
    }

    @Test
    void beanValidation_negativeBid_bidViolationRaised() {
        // Given — @Positive violated on bid
        StoreRateRequest request = new StoreRateRequest(
                "EUR/USD", "REUTERS",
                new BigDecimal("-1.00"),  // negative — violates @Positive
                new BigDecimal("1.2700"),
                new BigDecimal("1.2650")
        );

        // When
        Set<ConstraintViolation<StoreRateRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("bid")));
    }

    @Test
    void beanValidation_nullAsk_askViolationRaised() {
        // Given — @NotNull violated on ask; null ask also makes isValidSpread return true
        // (null-safe path), so only the @NotNull violation should fire
        StoreRateRequest request = new StoreRateRequest(
                "EUR/USD", "REUTERS",
                new BigDecimal("1.2600"),
                null,                    // null ask
                new BigDecimal("1.2650")
        );

        // When
        Set<ConstraintViolation<StoreRateRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("ask")));
    }
}

