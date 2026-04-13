package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.EodFixing;
import com.FXplore.fx_rate_service.model.CurrencyPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface EodFixingRepository extends JpaRepository<EodFixing, Integer> {

    Optional<EodFixing> findByPairAndFixingDate(CurrencyPair pair, LocalDate fixingDate);

    Optional<EodFixing> findByPairAndFixingDateAndIsOfficialTrue(
            CurrencyPair pair,
            LocalDate fixingDate
    );
}
