package com.FXplore.fx_rate_service.demo;

import com.FXplore.fx_rate_service.service.IRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Micro-demo: fetch the official EOD fixing for EUR/USD on 2026-03-25.
 * Run with: -Dspring.profiles.active=demo-eod-fixing
 */
@Component
@Profile("demo-eod-fixing")
@RequiredArgsConstructor
public class DemoEodFixing implements CommandLineRunner {

    private final IRateService rateService;

    @Override
    public void run(String... args) {
        System.out.println("\n=== [demo-eod-fixing] getEodFixing ===");
        try {
            rateService.getEodFixing("EUR/USD", LocalDate.of(2026, 3, 25)).ifPresentOrElse(
                    f -> System.out.println(
                            "EOD fixing EUR/USD on 2026-03-25: rate=" + f.fixingRate()
                            + " type=" + f.fixingType()
                            + " official=" + f.isOfficial()),
                    () -> System.out.println("No EOD fixing found for EUR/USD on 2026-03-25")
            );
        } catch (RuntimeException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }
}

