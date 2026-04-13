package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.RateAuditLog;
import com.FXplore.fx_rate_service.model.CurrencyPair;

import java.util.List;

public interface IRateAuditLogRepository {

    List<RateAuditLog> findByPairOrderByChangedAtDesc(CurrencyPair pair);
}
