package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.CurrencyPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ICurrencyPairRepository extends JpaRepository<CurrencyPair, Integer> {

    Optional<CurrencyPair> findByPairCode(String pairCode);

    Optional<CurrencyPair> findByBaseCurrency_IsoCodeAndQuoteCurrency_IsoCode(
            String baseCurrency,
            String quoteCurrency
    );
}