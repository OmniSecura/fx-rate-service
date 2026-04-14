package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.RateAlert;
import com.FXplore.fx_rate_service.model.CurrencyPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IRateAlertRepository extends JpaRepository<RateAlert, Integer> {

    List<RateAlert> findByPairAndStatus(CurrencyPair pair, String status);

    List<RateAlert> findByPairOrderByTriggeredAtDesc(CurrencyPair pair);
}