package com.FXplore.fx_rate_service.controller;

import com.FXplore.fx_rate_service.dto.StoreRateRequest;
import com.FXplore.fx_rate_service.model.Currency;
import com.FXplore.fx_rate_service.model.EodFixing;
import com.FXplore.fx_rate_service.model.ExchangeRate;
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
     *
     * Example request body:
     * {
     *   "pairCode":     "GBPUSD",
     *   "providerCode": "REUTERS",
     *   "bid":          1.2645,
     *   "ask":          1.2648,
     *   "mid":          1.2647
     * }
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
     * Declared before /{pair} to avoid route ambiguity.
     */
    @GetMapping("/rates/stale")
    public ResponseEntity<List<ExchangeRate>> getStaleRates() {
        return ResponseEntity.ok(rateService.getStaleRates());
    }

    /**
     * GET /api/rates/{pair}
     * Get the latest rate for a currency pair (e.g. GBPUSD).
     */
    @GetMapping("/rates/{pair}")
    public ResponseEntity<ExchangeRate> getLatestRate(@PathVariable String pair) {
        return rateService.getLatestRate(pair)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/rates/{pair}/history?from=2026-01-01&to=2026-04-16
     * Get rate history for a pair within a date range.
     */
    @GetMapping("/rates/{pair}/history")
    public ResponseEntity<List<ExchangeRate>> getRateHistory(
            @PathVariable String pair,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(rateService.getRateHistory(pair, from, to));
    }

    /**
     * GET /api/convert?from=GBP&to=USD&amount=10000
     * Convert an amount between two currencies using the latest mid rate.
     */
    @GetMapping("/convert")
    public ResponseEntity<Map<String, Object>> convertAmount(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount) {
        String pairCode = from + to;
        return rateService.convertAmount(amount, pairCode)
                .<ResponseEntity<Map<String, Object>>>map(converted -> ResponseEntity.ok(Map.of(
                        "from", from,
                        "to", to,
                        "amount", amount,
                        "converted", converted,
                        "pair", pairCode
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/fixings/{pair}?date=2026-04-07
     * Get the official EOD fixing for a pair on a given date.
     */
    @GetMapping("/fixings/{pair}")
    public ResponseEntity<EodFixing> getEodFixing(
            @PathVariable String pair,
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
    public ResponseEntity<List<Currency>> getAllActiveCurrencies() {
        return ResponseEntity.ok(rateService.getAllActiveCurrencies());
    }
}

