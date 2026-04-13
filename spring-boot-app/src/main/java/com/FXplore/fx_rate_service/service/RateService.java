package com.FXplore.fx_rate_service.service;

import com.FXplore.fx_rate_service.model.CurrencyPair;
import com.FXplore.fx_rate_service.model.EodFixing;
import com.FXplore.fx_rate_service.model.ExchangeRate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RateService implements IRateService{
    @Override
    public void storeRate(ExchangeRate rate) {

    }

    @Override
    public Optional<ExchangeRate> getLatestRate(CurrencyPair pair) {
        return Optional.empty();
    }

    @Override
    public List<ExchangeRate> getRateHistory(CurrencyPair pair, LocalDate from, LocalDate to) {
        return List.of();
    }

    @Override
    public Optional<BigDecimal> calculateCrossRate(CurrencyPair pairAC, CurrencyPair pairAB, CurrencyPair pairBC) {
        return Optional.empty();
    }

    @Override
    public Optional<BigDecimal> convertAmount(BigDecimal amount, CurrencyPair pair) {
        return Optional.empty();
    }

    @Override
    public Optional<EodFixing> getEodFixing(CurrencyPair pair, LocalDate date) {
        return Optional.empty();
    }
}
