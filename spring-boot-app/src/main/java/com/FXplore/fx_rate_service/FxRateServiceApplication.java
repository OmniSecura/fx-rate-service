package com.FXplore.fx_rate_service;

import com.FXplore.fx_rate_service.config.AppConfiguration;
import com.FXplore.fx_rate_service.service.IRateService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootApplication
@Import(AppConfiguration.class)
public class FxRateServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FxRateServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner run(IRateService rateService) {
		return args -> {

			// --- 1. storeRate ---
			System.out.println("\n=== storeRate ===");
			try {
				rateService.storeRate("EUR/USD", "ECB",
						new BigDecimal("1.0820"),
						new BigDecimal("1.0840"),
						new BigDecimal("1.0830"));
				System.out.println("Rate stored successfully.");
			} catch (RuntimeException e) {
				System.out.println("Failed to store rate: " + e.getMessage());
			}

			// --- 2. getLatestRate ---
			System.out.println("\n=== getLatestRate ===");
			try {
				rateService.getLatestRate("EUR/USD").ifPresentOrElse(
						r -> System.out.println("Latest EUR/USD rate: mid=" + r.getMidRate() + " stale=" + r.getIsStale()),
						() -> System.out.println("No rate found for EUR/USD")
				);
			} catch (RuntimeException e) {
				System.out.println("Failed to get latest rate: " + e.getMessage());
			}

			// --- 3. getRateHistory ---
			System.out.println("\n=== getRateHistory ===");
			try {
				rateService.getRateHistory("EUR/USD", LocalDate.now().minusDays(7), LocalDate.now())
						.forEach(r -> System.out.println(
								"  " + r.getRateTimestamp() + " | bid=" + r.getBidRate() + " mid=" + r.getMidRate() + " ask=" + r.getAskRate()
						));
			} catch (RuntimeException e) {
				System.out.println("Failed to get rate history: " + e.getMessage());
			}

			// --- 4. calculateCrossRate ---
			System.out.println("\n=== calculateCrossRate ===");
			try {
				rateService.calculateCrossRate("EUR/GBP", "EUR/USD", "GBP/USD").ifPresentOrElse(
						r -> System.out.println("Cross rate EUR/GBP = " + r),
						() -> System.out.println("Insufficient data to calculate cross rate")
				);
			} catch (RuntimeException e) {
				System.out.println("Failed to calculate cross rate: " + e.getMessage());
			}

			// --- 5. convertAmount ---
			System.out.println("\n=== convertAmount ===");
			try {
				rateService.convertAmount(new BigDecimal("1000.00"), "EUR/USD").ifPresentOrElse(
						r -> System.out.println("1000 EUR = " + r + " USD"),
						() -> System.out.println("No rate available for conversion")
				);
			} catch (RuntimeException e) {
				System.out.println("Failed to convert amount: " + e.getMessage());
			}

			// --- 6. getEodFixing ---
			System.out.println("\n=== getEodFixing ===");
			try {
				rateService.getEodFixing("EUR/USD", LocalDate.now()).ifPresentOrElse(
						f -> System.out.println("EOD fixing EUR/USD: " + f.getFixingRate()),
						() -> System.out.println("No EOD fixing found for EUR/USD today")
				);
			} catch (RuntimeException e) {
				System.out.println("Failed to get EOD fixing: " + e.getMessage());
			}
		};
	}
}

