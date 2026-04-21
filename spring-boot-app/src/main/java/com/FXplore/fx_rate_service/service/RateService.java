package com.FXplore.fx_rate_service.service;

import com.FXplore.fx_rate_service.dao.ICurrencyPairRepository;
import com.FXplore.fx_rate_service.dao.ICurrencyRepository;
import com.FXplore.fx_rate_service.dao.IEodFixingRepository;
import com.FXplore.fx_rate_service.dao.IExchangeRateRepository;
import com.FXplore.fx_rate_service.dao.IRateProviderRepository;
import com.FXplore.fx_rate_service.dto.ConversionResponse;
import com.FXplore.fx_rate_service.dto.CurrencyResponse;
import com.FXplore.fx_rate_service.dto.EodFixingResponse;
import com.FXplore.fx_rate_service.dto.ExchangeRateResponse;
import com.FXplore.fx_rate_service.exception.CurrencyPairNotFoundException;
import com.FXplore.fx_rate_service.exception.InvalidExchangeRateException;
import com.FXplore.fx_rate_service.exception.RateProviderNotFoundException;
import com.FXplore.fx_rate_service.model.CurrencyPair;
import com.FXplore.fx_rate_service.model.ExchangeRate;
import com.FXplore.fx_rate_service.model.RateProvider;
import com.FXplore.fx_rate_service.validation.SpreadValidator;
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
        // Reuse the same spread rule enforced at the DTO layer (bid < mid < ask)
        if (!SpreadValidator.isValid(bid, mid, ask)) {
            throw new InvalidExchangeRateException("Bid/ask spread invalid: required bid < mid < ask");
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
    public Optional<ExchangeRateResponse> getLatestRate(String pairCode) {
        CurrencyPair pair = getCurrencyPairByCode(pairCode);
        Optional<ExchangeRate> latest = exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(pair);

        latest.ifPresent(rate -> {
            Instant now = Instant.now();
            boolean isStale = rate.getRateTimestamp().isBefore(now.minus(STALE_HOURS, ChronoUnit.HOURS));
            rate.setIsStale(isStale);
            if (isStale) {
                Duration age = Duration.between(rate.getRateTimestamp(), now);
                log.warn("Stale rate detected: pair={} lastUpdated={} age={}h {}m",
                        pairCode, rate.getRateTimestamp(), age.toHours(), age.minusHours(age.toHours()).toMinutes());
            }
        });

        return latest.map(ExchangeRateResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRateResponse> getRateHistory(String pairCode, LocalDate from, LocalDate to) {
        CurrencyPair pair = getCurrencyPairByCode(pairCode);
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = to.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        return exchangeRateRepository
                .findByPairAndRateTimestampBetweenOrderByRateTimestampDesc(pair, start, end)
                .stream()
                .map(ExchangeRateResponse::from)
                .toList();
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
    public Optional<ConversionResponse> convertAmount(BigDecimal amount, String pairCode) {
        String[] parts = pairCode.split("/");
        if (parts.length != 2) {
            return Optional.empty();
        }
        String from = parts[0];
        String to   = parts[1];

        // 1. Try direct pair FROM/TO
        Optional<ConversionResponse> direct = tryDirectConversion(amount, from, to, pairCode);
        if (direct.isPresent()) {
            return direct;
        }

        // 2. Fallback: cross rate via USD (FROM/USD × USD/TO)
        log.info("Direct pair {} not found, attempting cross rate via USD", pairCode);
        Optional<ConversionResponse> viaUsd = tryCrossConversion(amount, from, to, "USD");
        if (viaUsd.isPresent()) {
            return viaUsd;
        }

        // 3. Fallback: cross rate via EUR (FROM/EUR × EUR/TO)
        log.info("Cross rate via USD not available for {}, attempting via EUR", pairCode);
        return tryCrossConversion(amount, from, to, "EUR");
    }

    /**
     * Attempt a direct conversion using an existing pair in the database.
     */
    private Optional<ConversionResponse> tryDirectConversion(BigDecimal amount, String from, String to, String pairCode) {
        return currencyPairRepository.findByPairCode(pairCode)
                .flatMap(pair -> exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(pair)
                        .map(rate -> {
                            BigDecimal converted = amount.multiply(rate.getMidRate()).setScale(6, RoundingMode.HALF_UP);
                            log.info("Direct conversion: {} {} -> {} {} rate={}", amount, from, converted, to, rate.getMidRate());
                            return new ConversionResponse(from, to, amount, converted, rate.getMidRate(), pairCode);
                        }));
    }

    /**
     * Attempt a cross rate conversion via an intermediate currency.
     * E.g. GBP/JPY via USD = GBP/USD × USD/JPY
     * Handles both FROM/VIA and VIA/FROM (inverted) pairs automatically.
     */
    private Optional<ConversionResponse> tryCrossConversion(BigDecimal amount, String from, String to, String via) {
        if (from.equals(via) || to.equals(via)) {
            return Optional.empty();
        }

        Optional<BigDecimal> rateFromVia = getMidRate(from, via);
        Optional<BigDecimal> rateViaTo   = getMidRate(via, to);

        if (rateFromVia.isEmpty() || rateViaTo.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal crossRate = rateFromVia.get()
                .multiply(rateViaTo.get())
                .setScale(6, RoundingMode.HALF_UP);

        BigDecimal converted = amount.multiply(crossRate).setScale(6, RoundingMode.HALF_UP);
        String syntheticPair = from + "/" + to;

        log.info("Cross rate conversion: {} {} -> {} {} via {} rate={} ({}{}={}, {}{}={})",
                amount, from, converted, to, via,
                crossRate, from, via, rateFromVia.get(), via, to, rateViaTo.get());

        return Optional.of(new ConversionResponse(from, to, amount, converted, crossRate, syntheticPair));
    }

    /**
     * Look up the mid rate for a pair, trying both FROM/TO and TO/FROM (inverted).
     */
    private Optional<BigDecimal> getMidRate(String base, String quote) {
        // Try direct: BASE/QUOTE
        Optional<BigDecimal> direct = currencyPairRepository.findByPairCode(base + "/" + quote)
                .flatMap(pair -> exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(pair))
                .map(ExchangeRate::getMidRate);
        if (direct.isPresent()) {
            return direct;
        }
        // Try inverted: QUOTE/BASE → rate = 1 / rate
        return currencyPairRepository.findByPairCode(quote + "/" + base)
                .flatMap(pair -> exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(pair))
                .map(rate -> BigDecimal.ONE.divide(rate.getMidRate(), 6, RoundingMode.HALF_UP));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EodFixingResponse> getEodFixing(String pairCode, LocalDate date) {
        CurrencyPair pair = getCurrencyPairByCode(pairCode);
        return eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(pair, date)
                .map(f -> {
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

                    return EodFixingResponse.from(f);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRateResponse> getStaleRates() {
        Instant threshold = Instant.now().minus(STALE_HOURS, ChronoUnit.HOURS);
        return exchangeRateRepository.findStaleRates(threshold)
                .stream()
                .peek(rate -> {
                    rate.setIsStale(true);
                    Duration age = Duration.between(rate.getRateTimestamp(), Instant.now());
                    log.warn("Stale rate detected: pair={} lastUpdated={} age={}h {}m",
                            rate.getPair().getPairCode(), rate.getRateTimestamp(),
                            age.toHours(), age.minusHours(age.toHours()).toMinutes());
                })
                .map(ExchangeRateResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrencyResponse> getAllActiveCurrencies() {
        return currencyRepository.findByIsActiveTrue()
                .stream()
                .map(CurrencyResponse::from)
                .toList();
    }
}