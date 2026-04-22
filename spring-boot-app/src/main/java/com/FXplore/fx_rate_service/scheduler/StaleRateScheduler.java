package com.FXplore.fx_rate_service.scheduler;

import com.FXplore.fx_rate_service.dto.ExchangeRateResponse;
import com.FXplore.fx_rate_service.service.IRateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled job that checks for stale FX rates every 15 minutes.
 *
 * A rate is considered stale when it has not been updated in over 4 hours.
 * "Rate staleness scheduled check every 15 minutes (@Scheduled)"
 *
 * The check runs automatically in the background — no HTTP request required.
 * Results are written to the application log so operations teams can monitor them.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class StaleRateScheduler {

    private static final Logger log = LoggerFactory.getLogger(StaleRateScheduler.class);


    private final IRateService rateService;

    /**
     * Runs every 15 minutes (900 000 ms).
     * Queries all currency pairs whose latest rate is older than 4 hours
     * and logs a WARN entry for each one.
     */
    @Scheduled(fixedRate = 900_000)
    public void checkStaleRates() {
        log.info("Scheduled stale-rate check started");

        List<ExchangeRateResponse> staleRates = rateService.getStaleRates();

        if (staleRates.isEmpty()) {
            log.info("Scheduled stale-rate check complete — all rates are fresh");
            return;
        }

        log.warn("Scheduled stale-rate check: {} pair(s) have stale rates", staleRates.size());
        for (ExchangeRateResponse rate : staleRates) {
            log.warn("Stale rate detected: pair={} lastUpdated={} provider={}",
                    rate.pairCode(),
                    rate.rateTimestamp(),
                    rate.providerCode());
        }
    }
}

