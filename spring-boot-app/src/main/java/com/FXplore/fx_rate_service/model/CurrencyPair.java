package com.FXplore.fx_rate_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "currency_pair")
public class CurrencyPair implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pair_id", nullable = false)
    private Integer id;

    @Column(name = "pair_code", nullable = false, length = 7)
    private String pairCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "base_currency", nullable = false, referencedColumnName = "iso_code")
    private Currency baseCurrency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_currency", nullable = false, referencedColumnName = "iso_code")
    private Currency quoteCurrency;

    @Column(name = "pair_type", nullable = false, length = 15)
    private String pairType;

    @ColumnDefault("4")
    @Column(name = "decimal_places", nullable = false)
    private Short decimalPlaces;

    @ColumnDefault("0.000100")
    @Column(name = "pip_size", nullable = false, precision = 10, scale = 6)
    private BigDecimal pipSize;

    @ColumnDefault("1")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @OneToMany(mappedBy = "pair")
    private Set<EodFixing> eodFixings = new LinkedHashSet<>();

    @OneToMany(mappedBy = "pair")
    private Set<ExchangeRate> exchangeRates = new LinkedHashSet<>();

    @OneToMany(mappedBy = "pair")
    private Set<ForwardRate> forwardRates = new LinkedHashSet<>();

    @OneToMany(mappedBy = "pair")
    private Set<RateAlert> rateAlerts = new LinkedHashSet<>();

    @OneToMany(mappedBy = "pair")
    private Set<RateAuditLog> rateAuditLogs = new LinkedHashSet<>();

}