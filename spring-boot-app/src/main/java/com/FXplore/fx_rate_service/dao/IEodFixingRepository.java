package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.EodFixing;
import com.FXplore.fx_rate_service.model.CurrencyPair;

import java.time.LocalDate;
import java.util.Optional;

public interface IEodFixingRepository {

    Optional<EodFixing> findByPairAndFixingDate(CurrencyPair pair, LocalDate fixingDate);

    Optional<EodFixing> findByPairAndFixingDateAndIsOfficialTrue(
            CurrencyPair pair,
            LocalDate fixingDate
    );
}
