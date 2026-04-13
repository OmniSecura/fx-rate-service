package com.FXplore.fx_rate_service.dao;

import com.FXplore.fx_rate_service.model.RateProvider;

import java.util.Optional;

public interface IRateProviderDAO {
     //Returns all providers
    Optional<RateProvider> findAll();
    //Returns one provider by its code
    Optional<RateProvider> findByCode(String code);
    //Returns only active providers
    Optional<RateProvider> findAllActive(Boolean isActive);



}
