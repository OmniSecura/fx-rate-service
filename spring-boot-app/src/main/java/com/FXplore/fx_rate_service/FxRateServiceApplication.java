package com.FXplore.fx_rate_service;

import com.FXplore.fx_rate_service.config.AppConfiguration;
import com.FXplore.fx_rate_service.service.IRateService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootApplication
@Import(AppConfiguration.class)
@EnableScheduling
public class FxRateServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FxRateServiceApplication.class, args);
	}

	// -------------------------------------------------------------------------
	// DemoAll — starts akk service methods with sample data and prints results to console.
	// -------------------------------------------------------------------------
	@Bean
	CommandLineRunner demoAll(IRateService rateService) {
		return args -> {
			// --- 1. storeRate ---
			// Stores a new exchange rate for EUR/USD provided by ECB.
			System.out.println("\n=== [1] storeRate ===");
			try {
				rateService.storeRate(
				"EUR/USD", "ECB",
				new BigDecimal("1.0820"),  // bid
				new BigDecimal("1.0840"),  // ask
				new BigDecimal("1.0830")   // mid
			);
				System.out.println("Rate stored successfully.");
			} catch (RuntimeException e) {
				System.out.println("Failed to store rate: " + e.getMessage());
			}

			// --- 2. getLatestRate ---
			// Retrieves the most recent EUR/USD rate and checks if it is stale.
			System.out.println("\n=== [2] getLatestRate ===");
			try {
				rateService.getLatestRate("EUR/USD").ifPresentOrElse(
					r -> System.out.println(
						"Latest EUR/USD rate: bid=" + r.bidRate()
						+ " mid=" + r.midRate()
						+ " ask=" + r.askRate()
						+ " | stale=" + r.isStale()),
					() -> System.out.println("No rate found for EUR/USD")
				);
			} catch (RuntimeException e) {
				System.out.println("Failed to get latest rate: " + e.getMessage());
			}

			// --- 3. getRateHistory ---
			// Lists all EUR/USD rates between 2026-03-26 and today.
			System.out.println("\n=== [3] getRateHistory ===");
			try {
				LocalDate from = LocalDate.of(2026, 3, 26);
				LocalDate to   = LocalDate.now();
				rateService.getRateHistory("EUR/USD", from, to)
					.forEach(r -> System.out.println(
						"  " + r.rateTimestamp()
						+ " | bid=" + r.bidRate()
						+ " mid=" + r.midRate()
						+ " ask=" + r.askRate()));
			} catch (RuntimeException e) {
				System.out.println("Failed to get rate history: " + e.getMessage());
			}

			// --- 4. calculateCrossRate ---
			// Calculates the cross rate for EUR/JPY using EUR/USD and USD/JPY.
			System.out.println("\n=== [4] calculateCrossRate ===");
			try {
				rateService.calculateCrossRate("EUR/JPY", "EUR/USD", "USD/JPY").ifPresentOrElse(
					r -> System.out.println("Cross rate EUR/JPY = " + r),
					() -> System.out.println("Insufficient data to calculate cross rate")
				);
			} catch (RuntimeException e) {
				System.out.println("Failed to calculate cross rate: " + e.getMessage());
			}

			// --- 5. convertAmount ---
			// Converts 1000 EUR to USD using the latest EUR/USD rate.
			System.out.println("\n=== [5] convertAmount ===");
			try {
				rateService.convertAmount(new BigDecimal("1000.00"), "EUR/USD").ifPresentOrElse(
					r -> System.out.println("1000 EUR = " + r.convertedAmount() + " USD"),
					() -> System.out.println("No rate available for conversion")
				);
			} catch (RuntimeException e) {
				System.out.println("Failed to convert amount: " + e.getMessage());
			}

			// --- 6. getEodFixing ---
			// Retrieves the official end-of-day fixing for EUR/USD on 2026-03-25.
			System.out.println("\n=== [6] getEodFixing ===");
			try {
				rateService.getEodFixing("EUR/USD", LocalDate.of(2026, 3, 25)).ifPresentOrElse(
					f -> System.out.println(
						"EOD fixing EUR/USD on 2026-03-25: rate=" + f.fixingRate()
						+ " type=" + f.fixingType()
						+ " official=" + f.isOfficial()),
					() -> System.out.println("No EOD fixing found for EUR/USD on 2026-03-25")
				);
			} catch (RuntimeException e) {
				System.out.println("Failed to get EOD fixing: " + e.getMessage());
			}
		};
	}
}
