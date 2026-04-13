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
import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "eod_fixing")
public class EodFixing implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fixing_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_id", nullable = false)
    private CurrencyPair pair;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private RateProvider provider;

    @Column(name = "fixing_date", nullable = false)
    private LocalDate fixingDate;

    @Column(name = "fixing_rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal fixingRate;

    @Column(name = "fixing_time", nullable = false, length = 10)
    private String fixingTime;

    @Column(name = "fixing_type", nullable = false, length = 20)
    private String fixingType;

    @ColumnDefault("1")
    @Column(name = "is_official", nullable = false)
    private Boolean isOfficial = false;

    @ColumnDefault("current_timestamp()")
    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

}