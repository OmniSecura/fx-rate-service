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
import java.time.Duration;
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
    private static final BigDecimal FIXING_DEVIATION_ALERT_THRESHOLD_PCT = new BigDecimal("0.50");

    @Override
    public CurrencyPair getCurrencyPairByCode(String pairCode) {
        return currencyPairRepository.findByPairCode(pairCode)
                .orElseThrow(() -> {
                    log.error("Unknown currency pair: {} not found in reference data", pairCode);
                    return new CurrencyPairNotFoundException("Currency pair not found: " + pairCode);
                });
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
            Instant now = Instant.now();
            boolean isStale = rate.getRateTimestamp()
                    .isBefore(now.minus(STALE_HOURS, ChronoUnit.HOURS));
            rate.setIsStale(isStale);
            if (isStale) {
                Duration age = Duration.between(rate.getRateTimestamp(), now);
                log.warn("Stale rate detected: pair={} lastUpdated={} age={}h {}m",
                        pairCode, rate.getRateTimestamp(), age.toHours(), age.minusHours(age.toHours()).toMinutes());
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

        log.info("Cross rate calculated: {}={} {}={} -> {}={}",
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
                    log.info("Conversion: {} {} -> {} {} rate={} asOf={}",
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
        Optional<EodFixing> fixing = eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(pair, date);

        fixing.ifPresent(f -> {
            String source = f.getProvider() != null ? f.getProvider().getProviderCode() : "UNKNOWN";
            log.info("EOD fixing stored: pair={} date={} fixingRate={} source={}",
                    pairCode, f.getFixingDate(), f.getFixingRate(), source);

            exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(pair).ifPresent(latest -> {
                BigDecimal lastTraded = latest.getMidRate();
                if (lastTraded != null && lastTraded.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal deviationPct = f.getFixingRate()
                            .subtract(lastTraded)
                            .abs()
                            .divide(lastTraded, 6, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP);

                    if (deviationPct.compareTo(FIXING_DEVIATION_ALERT_THRESHOLD_PCT) > 0) {
                        log.warn("Fixing deviation alert: pair={} fixing={} lastTraded={} deviation={}%%",
                                pairCode, f.getFixingRate(), lastTraded, deviationPct);
                    }
                }
            });
        });

        return fixing;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRate> getStaleRates() {
        Instant threshold = Instant.now().minus(STALE_HOURS, ChronoUnit.HOURS);
        List<ExchangeRate> staleRates = exchangeRateRepository.findStaleRates(threshold);
        staleRates.forEach(rate -> {
            rate.setIsStale(true);
            Duration age = Duration.between(rate.getRateTimestamp(), Instant.now());
            log.warn("Stale rate detected: pair={} lastUpdated={} age={}h {}m",
                    rate.getPair().getPairCode(), rate.getRateTimestamp(), age.toHours(), age.minusHours(age.toHours()).toMinutes());
        });
        return staleRates;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Currency> getAllActiveCurrencies() {
        return currencyRepository.findByIsActiveTrue();
    }
}
