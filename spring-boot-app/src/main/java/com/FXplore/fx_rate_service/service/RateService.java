package com.FXplore.fx_rate_service.service;

import com.FXplore.fx_rate_service.dao.ICurrencyPairRepository;
import com.FXplore.fx_rate_service.dao.IEodFixingRepository;
import com.FXplore.fx_rate_service.dao.IExchangeRateRepository;
import com.FXplore.fx_rate_service.dao.IRateProviderRepository;
import com.FXplore.fx_rate_service.exception.CurrencyPairNotFoundException;
import com.FXplore.fx_rate_service.exception.InvalidExchangeRateException;
import com.FXplore.fx_rate_service.exception.RateProviderNotFoundException;
import com.FXplore.fx_rate_service.model.CurrencyPair;
import com.FXplore.fx_rate_service.model.EodFixing;
import com.FXplore.fx_rate_service.model.ExchangeRate;
import com.FXplore.fx_rate_service.model.RateProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RateService implements IRateService {

    private final IExchangeRateRepository exchangeRateRepository;
    private final IEodFixingRepository eodFixingRepository;
    private final ICurrencyPairRepository currencyPairRepository;
    private final IRateProviderRepository rateProviderRepository;

    private static final int STALE_HOURS = 4;

    @Override
    public CurrencyPair getCurrencyPairByCode(String pairCode) {
        return currencyPairRepository.findByPairCode(pairCode)
                .orElseThrow(() -> new CurrencyPairNotFoundException("Currency pair not found: " + pairCode));
    }

    @Override
    public RateProvider getRateProviderByCode(String providerCode) {
        return rateProviderRepository.findByProviderCode(providerCode)
                .orElseThrow(() -> new RateProviderNotFoundException("Rate provider not found: " + providerCode));
    }

    @Override
    @Transactional
    public void storeRate(String pairCode, String providerCode, BigDecimal bid, BigDecimal ask, BigDecimal mid) {
        if (bid == null || ask == null || mid == null) {
            throw new InvalidExchangeRateException("Exchange rates cannot be null");
        }
        if (bid.compareTo(BigDecimal.ZERO) <= 0 || ask.compareTo(BigDecimal.ZERO) <= 0 || mid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidExchangeRateException("Exchange rates must be positive");
        }
        if (bid.compareTo(ask) >= 0) {
            throw new InvalidExchangeRateException("Bid rate must be less than ask rate");
        }

        CurrencyPair pair = getCurrencyPairByCode(pairCode);
        RateProvider provider = getRateProviderByCode(providerCode);

        ExchangeRate rate = new ExchangeRate();
        rate.setPair(pair);
        rate.setProvider(provider);
        rate.setBidRate(bid);
        rate.setAskRate(ask);
        rate.setMidRate(mid);
        rate.setRateTimestamp(Instant.now());
        rate.setSourceSystem(provider.getProviderCode().equals("ECB") ? "ECB_FEED" : provider.getProviderCode());
        rate.setIsValid(true);
        rate.setIsStale(false);

        exchangeRateRepository.save(rate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExchangeRate> getLatestRate(String pairCode) {
        CurrencyPair pair = getCurrencyPairByCode(pairCode);
        Optional<ExchangeRate> latest = exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(pair);

        latest.ifPresent(rate -> {
            boolean isStale = rate.getRateTimestamp()
                    .isBefore(Instant.now().minus(STALE_HOURS, ChronoUnit.HOURS));
            rate.setIsStale(isStale);
        });

        return latest;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRate> getRateHistory(String pairCode, LocalDate from, LocalDate to) {
        CurrencyPair pair = getCurrencyPairByCode(pairCode);
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = to.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        return exchangeRateRepository.findByPairAndRateTimestampBetweenOrderByRateTimestampDesc(pair, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> calculateCrossRate(String pairCodeAC, String pairCodeAB, String pairCodeBC) {
        CurrencyPair pairAB = getCurrencyPairByCode(pairCodeAB);
        CurrencyPair pairBC = getCurrencyPairByCode(pairCodeBC);

        Optional<ExchangeRate> rateAB = exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(pairAB);
        Optional<ExchangeRate> rateBC = exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(pairBC);

        if (rateAB.isEmpty() || rateBC.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal crossRate = rateAB.get().getMidRate()
                .multiply(rateBC.get().getMidRate())
                .setScale(6, RoundingMode.HALF_UP);

        return Optional.of(crossRate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> convertAmount(BigDecimal amount, String pairCode) {
        CurrencyPair pair = getCurrencyPairByCode(pairCode);
        return exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(pair)
                .map(rate -> amount.multiply(rate.getMidRate()).setScale(6, RoundingMode.HALF_UP));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EodFixing> getEodFixing(String pairCode, LocalDate date) {
        CurrencyPair pair = getCurrencyPairByCode(pairCode);
        return eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(pair, date);
    }
}
