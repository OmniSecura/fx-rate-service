package com.FXplore.fx_rate_service.controller;

import com.FXplore.fx_rate_service.dto.ConversionResponse;
import com.FXplore.fx_rate_service.dto.CurrencyResponse;
import com.FXplore.fx_rate_service.dto.EodFixingResponse;
import com.FXplore.fx_rate_service.dto.ExchangeRateResponse;
import com.FXplore.fx_rate_service.dto.StoreRateRequest;
import com.FXplore.fx_rate_service.service.IRateService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RateController {

    private final IRateService rateService;

    public RateController(IRateService rateService) {
        this.rateService = rateService;
    }

    /**
     * POST /api/rates
     * Store a new exchange rate.
     */
    @PostMapping("/rates")
    public ResponseEntity<Map<String, String>> storeRate(@Valid @RequestBody StoreRateRequest request) {
        rateService.storeRate(
                request.pairCode(),
                request.providerCode(),
                request.bid(),
                request.ask(),
                request.mid()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", "Rate stored successfully for pair " + request.pairCode()));
    }

    /**
     * GET /api/rates/stale
     * List all pairs with stale rates (no update in 4+ hours).
     */
    @GetMapping("/rates/stale")
    public ResponseEntity<List<ExchangeRateResponse>> getStaleRates() {
        return ResponseEntity.ok(rateService.getStaleRates());
    }

    /**
     * GET /api/rates?pair=EUR/USD
     * Get the latest rate for a currency pair (e.g. EUR/USD).
     */
    @GetMapping("/rates")
    public ResponseEntity<ExchangeRateResponse> getLatestRate(@RequestParam String pair) {
        return rateService.getLatestRate(pair)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/rates/history?pair=EUR/USD&from=2026-01-01&to=2026-04-16
     * Get rate history for a pair within a date range.
     */
    @GetMapping("/rates/history")
    public ResponseEntity<List<ExchangeRateResponse>> getRateHistory(
            @RequestParam String pair,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(rateService.getRateHistory(pair, from, to));
    }

    /**
     * GET /api/convert?from=GBP&to=USD&amount=10000
     * Convert an amount between two currencies using the latest mid rate.
     */
    @GetMapping("/convert")
    public ResponseEntity<ConversionResponse> convertAmount(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount) {
        String pairCode = from + "/" + to;
        return rateService.convertAmount(amount, pairCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/fixings?pair=EUR/USD&date=2026-04-07
     * Get the official EOD fixing for a pair on a given date.
     */
    @GetMapping("/fixings")
    public ResponseEntity<EodFixingResponse> getEodFixing(
            @RequestParam String pair,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return rateService.getEodFixing(pair, date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/currencies
     * List all active currencies.
     */
    @GetMapping("/currencies")
    public ResponseEntity<List<CurrencyResponse>> getAllActiveCurrencies() {
        return ResponseEntity.ok(rateService.getAllActiveCurrencies());
    }
}