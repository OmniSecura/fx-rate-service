package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.ForwardRate;
import com.FXplore.fx_rate_service.model.CurrencyPair;

import java.time.LocalDate;
import java.util.List;

public interface IForwardRateRepository {

    List<ForwardRate> findByPairAndValueDate(CurrencyPair pair, LocalDate valueDate);

    List<ForwardRate> findByPairOrderByValueDateAsc(CurrencyPair pair);
}
