package com.FXplore.fx_rate_service.service;

import com.FXplore.fx_rate_service.dao.ICurrencyPairRepository;
import com.FXplore.fx_rate_service.dao.ICurrencyRepository;
import com.FXplore.fx_rate_service.dao.IEodFixingRepository;
import com.FXplore.fx_rate_service.dao.IExchangeRateRepository;
import com.FXplore.fx_rate_service.dao.IRateProviderRepository;
import com.FXplore.fx_rate_service.exception.CurrencyPairNotFoundException;
import com.FXplore.fx_rate_service.exception.InvalidExchangeRateException;
import com.FXplore.fx_rate_service.exception.RateProviderNotFoundException;
import com.FXplore.fx_rate_service.model.Currency;
import com.FXplore.fx_rate_service.model.CurrencyPair;
import com.FXplore.fx_rate_service.model.EodFixing;
import com.FXplore.fx_rate_service.model.ExchangeRate;
import com.FXplore.fx_rate_service.model.RateProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(RateService.class);

    private final IExchangeRateRepository exchangeRateRepository;
    private final IEodFixingRepository eodFixingRepository;
    private final ICurrencyPairRepository currencyPairRepository;
    private final IRateProviderRepository rateProviderRepository;
    private final ICurrencyRepository currencyRepository;

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
        Instant now = Instant.now();
        rate.setRateTimestamp(now);
        rate.setSourceSystem(provider.getProviderCode().equals("ECB") ? "ECB_FEED" : provider.getProviderCode());
        rate.setIsValid(true);
        rate.setIsStale(false);

        exchangeRateRepository.save(rate);
        log.info("Rate stored: pair={} bid={} ask={} mid={} provider={} ts={}", pairCode, bid, ask, mid, providerCode, now);
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
            if (isStale) {
                log.warn("Stale rate detected: pair={} lastUpdated={} age={}h",
                        pairCode, rate.getRateTimestamp(), STALE_HOURS);
            }
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

        log.info("Cross rate calculated: {}={} {}={} → {}={}",
                pairCodeAB, rateAB.get().getMidRate(),
                pairCodeBC, rateBC.get().getMidRate(),
                pairCodeAC, crossRate);

        return Optional.of(crossRate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> convertAmount(BigDecimal amount, String pairCode) {
        CurrencyPair pair = getCurrencyPairByCode(pairCode);
        return exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(pair)
                .map(rate -> {
                    BigDecimal result = amount.multiply(rate.getMidRate()).setScale(6, RoundingMode.HALF_UP);
                    log.info("Conversion: {} {} → {} {} rate={} asOf={}",
                            amount, pair.getBaseCurrency().getIsoCode(),
                            result, pair.getQuoteCurrency().getIsoCode(),
                            rate.getMidRate(), rate.getRateTimestamp());
                    return result;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EodFixing> getEodFixing(String pairCode, LocalDate date) {
        CurrencyPair pair = getCurrencyPairByCode(pairCode);
        return eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(pair, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRate> getStaleRates() {
        Instant threshold = Instant.now().minus(STALE_HOURS, ChronoUnit.HOURS);
        List<ExchangeRate> staleRates = exchangeRateRepository.findStaleRates(threshold);
        staleRates.forEach(rate -> {
            rate.setIsStale(true);
            log.warn("Stale rate detected: pair={} lastUpdated={} age={}h",
                    rate.getPair().getPairCode(), rate.getRateTimestamp(), STALE_HOURS);
        });
        return staleRates;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Currency> getAllActiveCurrencies() {
        return currencyRepository.findByIsActiveTrue();
    }
}
