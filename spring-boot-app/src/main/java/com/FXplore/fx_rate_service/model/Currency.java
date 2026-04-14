package com.FXplore.fx_rate_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "currency")
public class Currency implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "currency_id", nullable = false)
    private Integer id;

    @Column(name = "iso_code", nullable = false, columnDefinition = "CHAR(3)")
    private String isoCode;

    @Column(name = "currency_name", nullable = false, length = 60)
    private String currencyName;

    @Column(name = "country", nullable = false, length = 60)
    private String country;

    @Column(name = "numeric_code", nullable = false, columnDefinition = "CHAR(3)")
    private String numericCode;

    @ColumnDefault("2")
    @Column(name = "minor_units", nullable = false)
    private Short minorUnits;

    @ColumnDefault("1")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "region", nullable = false, length = 20)
    private String region;

    @OneToMany(mappedBy = "baseCurrency")
    private Set<CurrencyPair> baseCurrencyPairs = new LinkedHashSet<>();

    @OneToMany(mappedBy = "quoteCurrency")
    private Set<CurrencyPair> quoteCurrencyPairs = new LinkedHashSet<>();

}