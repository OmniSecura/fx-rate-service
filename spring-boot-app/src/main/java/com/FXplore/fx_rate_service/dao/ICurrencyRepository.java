package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

    public interface ICurrencyRepository extends JpaRepository<Currency, Integer> {

        Optional<Currency> findByIsoCode(String isoCode);

        boolean existsByIsoCode(String isoCode);
    }
