package com.FXplore.fx_rate_service.demo;

import com.FXplore.fx_rate_service.service.IRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Micro-demo: fetch the latest EUR/USD rate and check staleness flag.
 * Run with: -Dspring.profiles.active=demo-latest-rate
 */
@Component
@Profile("demo-latest-rate")
@RequiredArgsConstructor
public class DemoLatestRate implements CommandLineRunner {

    private final IRateService rateService;

    @Override
    public void run(String... args) {
        System.out.println("\n=== [demo-latest-rate] getLatestRate ===");
        try {
            rateService.getLatestRate("EUR/USD").ifPresentOrElse(
                    r -> System.out.println(
                            "Latest EUR/USD: bid=" + r.bidRate()
                            + " mid=" + r.midRate()
                            + " ask=" + r.askRate()
                            + " | stale=" + r.isStale()),
                    () -> System.out.println("No rate found for EUR/USD")
            );
        } catch (RuntimeException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }
}

