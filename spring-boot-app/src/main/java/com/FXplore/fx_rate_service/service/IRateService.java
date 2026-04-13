package com.FXplore.fx_rate_service.service;

import com.FXplore.fx_rate_service.model.CurrencyPair;
import com.FXplore.fx_rate_service.model.EodFixing;
import com.FXplore.fx_rate_service.model.ExchangeRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IRateService {
    //Accept a new rate (bid/ask/mid) for a currency pair and persist it
    void storeRate(ExchangeRate rate);

     //Return the most recent rate for a pair, with a staleness flag if older than 4 hours
    Optional<ExchangeRate> getLatestRate(CurrencyPair pair);

    //Return rate history for a pair between two dates
    List<ExchangeRate> getRateHistory(CurrencyPair pair, LocalDate from, LocalDate to);

    //Derive the implied rate for pair A/C given rates A/B and B/C
    Optional<BigDecimal> calculateCrossRate(CurrencyPair pairAC, CurrencyPair pairAB, CurrencyPair pairBC);

    //Convert a given amount from one currency to another using the latest mid rate
    Optional<BigDecimal> convertAmount(BigDecimal amount, CurrencyPair pair);

   //Return the official end-of-day fixing for a pair on a given date
    Optional<EodFixing> getEodFixing(CurrencyPair pair, LocalDate date);
}
