package com.FXplore.fx_rate_service.demo;

import com.FXplore.fx_rate_service.service.IRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Micro-demo: store a new EUR/USD rate.
 * Run with: -Dspring.profiles.active=demo-store-rate
 */
@Component
@Profile("demo-store-rate")
@RequiredArgsConstructor
public class DemoStoreRate implements CommandLineRunner {

    private final IRateService rateService;

    @Override
    public void run(String... args) {
        System.out.println("\n=== [demo-store-rate] storeRate ===");
        try {
            rateService.storeRate(
                    "EUR/USD", "ECB",
                    new BigDecimal("1.0820"),
                    new BigDecimal("1.0840"),
                    new BigDecimal("1.0830")
            );
            System.out.println("Rate stored successfully.");
        } catch (RuntimeException e) {
            System.out.println("Failed to store rate: " + e.getMessage());
        }
    }
}

