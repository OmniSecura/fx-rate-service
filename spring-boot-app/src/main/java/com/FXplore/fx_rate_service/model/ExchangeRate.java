package com.FXplore.fx_rate_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exchange_rate")
public class ExchangeRate implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rate_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_id", nullable = false)
    private CurrencyPair pair;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private RateProvider provider;

    @Column(name = "bid_rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal bidRate;

    @Column(name = "ask_rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal askRate;

    @Column(name = "mid_rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal midRate;

    @ColumnDefault("current_timestamp()")
    @Column(name = "rate_timestamp", nullable = false)
    private Instant rateTimestamp;

    @Column(name = "source_system", nullable = false, length = 30)
    private String sourceSystem;

    @ColumnDefault("1")
    @Column(name = "is_valid", nullable = false)
    private Boolean isValid = false;

    @ColumnDefault("0")
    @Column(name = "is_stale", nullable = false)
    private Boolean isStale = false;

}