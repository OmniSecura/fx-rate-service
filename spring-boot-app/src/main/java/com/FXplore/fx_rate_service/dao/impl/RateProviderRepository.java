package com.FXplore.fx_rate_service.dao.impl;

import com.FXplore.fx_rate_service.dao.IRateProviderRepository;
import com.FXplore.fx_rate_service.model.RateProvider;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public class RateProviderRepository implements IRateProviderRepository {

    @Override
    public Optional<RateProvider> findByProviderCode(String providerCode) {
        try {
            RateProvider provider = new RateProvider();

        } catch (Exception e) {

        }
        return Optional.empty();
    }
}
