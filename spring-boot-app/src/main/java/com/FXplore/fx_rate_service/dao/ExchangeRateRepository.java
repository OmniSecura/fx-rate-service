package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.ExchangeRate;
import com.FXplore.fx_rate_service.model.CurrencyPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Integer> {

    //  Latest rate
    Optional<ExchangeRate> findTopByPairOrderByRateTimestampDesc(CurrencyPair pair);

    //  Rate history
    List<ExchangeRate> findByPairAndRateTimestampBetweenOrderByRateTimestampDesc(
            CurrencyPair pair,
            Instant start,
            Instant end
    );

    //  All rates for pair (optional)
    List<ExchangeRate> findByPairOrderByRateTimestampDesc(CurrencyPair pair);
}
