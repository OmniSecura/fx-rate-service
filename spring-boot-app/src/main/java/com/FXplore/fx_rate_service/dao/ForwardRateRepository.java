package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.ForwardRate;
import com.FXplore.fx_rate_service.model.CurrencyPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ForwardRateRepository extends JpaRepository<ForwardRate, Integer> {

    List<ForwardRate> findByPairAndValueDate(CurrencyPair pair, LocalDate valueDate);

    List<ForwardRate> findByPairOrderByValueDateAsc(CurrencyPair pair);
}
