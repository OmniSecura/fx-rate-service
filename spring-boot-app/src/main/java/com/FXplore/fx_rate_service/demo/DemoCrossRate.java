package com.FXplore.fx_rate_service.demo;

import com.FXplore.fx_rate_service.service.IRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Micro-demo: calculate the implied cross rate EUR/JPY from EUR/USD and USD/JPY.
 * Run with: -Dspring.profiles.active=demo-cross-rate
 */
@Component
@Profile("demo-cross-rate")
@RequiredArgsConstructor
public class DemoCrossRate implements CommandLineRunner {

    private final IRateService rateService;

    @Override
    public void run(String... args) {
        System.out.println("\n=== [demo-cross-rate] calculateCrossRate ===");
        try {
            rateService.calculateCrossRate("EUR/JPY", "EUR/USD", "USD/JPY").ifPresentOrElse(
                    r -> System.out.println("Cross rate EUR/JPY = " + r),
                    () -> System.out.println("Insufficient data to calculate cross rate")
            );
        } catch (RuntimeException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }
}

