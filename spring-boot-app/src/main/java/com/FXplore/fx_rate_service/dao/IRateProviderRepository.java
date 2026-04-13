package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.RateProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IRateProviderRepository {

    Optional<RateProvider> findByProviderCode(String providerCode);
}
