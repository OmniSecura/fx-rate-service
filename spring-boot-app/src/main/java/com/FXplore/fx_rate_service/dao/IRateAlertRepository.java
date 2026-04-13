package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.RateAlert;
import com.FXplore.fx_rate_service.model.CurrencyPair;


import java.util.List;

public interface IRateAlertRepository {

    List<RateAlert> findByPairAndStatus(String status);

    List<RateAlert> findByPairOrderByTriggeredAtDesc(CurrencyPair pair);
}