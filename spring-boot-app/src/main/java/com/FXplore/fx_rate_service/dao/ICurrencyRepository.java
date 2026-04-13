package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.Currency;

import java.util.Optional;

    public interface ICurrencyRepository  {

        Optional<Currency> findByIsoCode(String isoCode);

        boolean existsByIsoCode(String isoCode);
    }
