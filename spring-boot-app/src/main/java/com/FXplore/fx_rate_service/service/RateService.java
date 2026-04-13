package com.FXplore.fx_rate_service.service;

import com.FXplore.fx_rate_service.dao.IEodFixingRepository;
import com.FXplore.fx_rate_service.dao.IExchangeRateRepository;
import com.FXplore.fx_rate_service.model.CurrencyPair;
import com.FXplore.fx_rate_service.model.EodFixing;
import com.FXplore.fx_rate_service.model.ExchangeRate;
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

    private static final int STALE_HOURS = 4;

    @Override
    @Transactional
    public void storeRate(ExchangeRate rate) {
        exchangeRateRepository.save(rate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExchangeRate> getLatestRate(CurrencyPair pair) {
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
    public List<ExchangeRate> getRateHistory(CurrencyPair pair, LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = to.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        return exchangeRateRepository.findByPairAndRateTimestampBetweenOrderByRateTimestampDesc(pair, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> calculateCrossRate(CurrencyPair pairAC, CurrencyPair pairAB, CurrencyPair pairBC) {
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
    public Optional<BigDecimal> convertAmount(BigDecimal amount, CurrencyPair pair) {
        return exchangeRateRepository.findTopByPairOrderByRateTimestampDesc(pair)
                .map(rate -> amount.multiply(rate.getMidRate()).setScale(6, RoundingMode.HALF_UP));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EodFixing> getEodFixing(CurrencyPair pair, LocalDate date) {
        return eodFixingRepository.findByPairAndFixingDateAndIsOfficialTrue(pair, date);
    }
}
