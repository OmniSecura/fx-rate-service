package com.FXplore.fx_rate_service.demo;

import com.FXplore.fx_rate_service.service.IRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Micro-demo: fetch EUR/USD rate history between two dates.
 * Run with: -Dspring.profiles.active=demo-rate-history
 */
@Component
@Profile("demo-rate-history")
@RequiredArgsConstructor
public class DemoRateHistory implements CommandLineRunner {

    private final IRateService rateService;

    @Override
    public void run(String... args) {
        System.out.println("\n=== [demo-rate-history] getRateHistory ===");
        try {
            LocalDate from = LocalDate.of(2026, 3, 26);
            LocalDate to   = LocalDate.now();
            var history = rateService.getRateHistory("EUR/USD", from, to);
            if (history.isEmpty()) {
                System.out.println("No history found for EUR/USD between " + from + " and " + to);
                return;
            }
            history.forEach(r -> System.out.println(
                    "  " + r.rateTimestamp()
                    + " | bid=" + r.bidRate()
                    + " mid=" + r.midRate()
                    + " ask=" + r.askRate()));
        } catch (RuntimeException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }
}

