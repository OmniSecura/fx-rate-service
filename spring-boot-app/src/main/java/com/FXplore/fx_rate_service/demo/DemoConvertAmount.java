package com.FXplore.fx_rate_service.demo;

import com.FXplore.fx_rate_service.service.IRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Micro-demo: convert 1000 EUR to USD using the latest mid rate.
 * Run with: -Dspring.profiles.active=demo-convert
 */
@Component
@Profile("demo-convert")
@RequiredArgsConstructor
public class DemoConvertAmount implements CommandLineRunner {

    private final IRateService rateService;

    @Override
    public void run(String... args) {
        System.out.println("\n=== [demo-convert] convertAmount ===");
        try {
            rateService.convertAmount(new BigDecimal("1000.00"), "EUR/USD").ifPresentOrElse(
                    r -> System.out.println("1000 EUR = " + r.convertedAmount() + " USD"),
                    () -> System.out.println("No rate available for conversion")
            );
        } catch (RuntimeException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }
}

