package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.CurrencyPair;


import java.util.Optional;

public interface ICurrencyPairRepository {

    Optional<CurrencyPair> findByPairCode(String pairCode);

    Optional<CurrencyPair> findByBaseCurrency_IsoCodeAndQuoteCurrency_IsoCode(
            String baseCurrency,
            String quoteCurrency
    );
}