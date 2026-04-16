package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.ExchangeRate;
import com.FXplore.fx_rate_service.model.CurrencyPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IExchangeRateRepository extends JpaRepository<ExchangeRate, Integer> {

    //  Latest rate
    Optional<ExchangeRate> findTopByPairOrderByRateTimestampDesc(CurrencyPair pair);

    //  Rate history
    List<ExchangeRate> findByPairAndRateTimestampBetweenOrderByRateTimestampDesc(
            CurrencyPair pair,
            Instant start,
            Instant end
    );

    // Latest rate per pair that is older than the stale threshold
    @Query("""
        SELECT e FROM ExchangeRate e
        WHERE e.rateTimestamp = (
            SELECT MAX(e2.rateTimestamp) FROM ExchangeRate e2 WHERE e2.pair = e.pair
        )
        AND e.rateTimestamp < :threshold
        """)
    List<ExchangeRate> findStaleRates(@Param("threshold") Instant threshold);
}
