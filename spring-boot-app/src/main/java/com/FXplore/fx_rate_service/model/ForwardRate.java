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
@Table(name = "forward_rate")
public class ForwardRate implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forward_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_id", nullable = false)
    private CurrencyPair pair;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private RateProvider provider;

    @Column(name = "tenor", nullable = false, length = 5)
    private String tenor;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Column(name = "forward_points", nullable = false, precision = 10, scale = 4)
    private BigDecimal forwardPoints;

    @Column(name = "forward_rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal forwardRate;

    @ColumnDefault("current_timestamp()")
    @Column(name = "rate_timestamp", nullable = false)
    private Instant rateTimestamp;

}