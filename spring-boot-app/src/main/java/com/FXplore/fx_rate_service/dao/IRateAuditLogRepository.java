package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.RateAuditLog;
import com.FXplore.fx_rate_service.model.CurrencyPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IRateAuditLogRepository extends JpaRepository<RateAuditLog, Integer> {

    List<RateAuditLog> findByPairOrderByChangedAtDesc(CurrencyPair pair);
}
