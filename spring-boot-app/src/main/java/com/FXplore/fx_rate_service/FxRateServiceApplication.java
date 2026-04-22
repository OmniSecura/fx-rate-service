package com.FXplore.fx_rate_service;

import com.FXplore.fx_rate_service.config.AppConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Import(AppConfiguration.class)
@EnableScheduling
public class FxRateServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FxRateServiceApplication.class, args);
	}
}
