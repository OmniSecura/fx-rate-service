package com.FXplore.fx_rate_service;

import com.FXplore.fx_rate_service.model.EodFixing;
import com.FXplore.fx_rate_service.model.ExchangeRate;
import com.FXplore.fx_rate_service.model.ForwardRate;
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
@Table(name = "rate_provider")
public class RateProvider implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id", nullable = false)
    private Integer id;

    @Column(name = "provider_code", nullable = false, length = 15)
    private String providerCode;

    @Column(name = "provider_name", nullable = false, length = 80)
    private String providerName;

    @Column(name = "provider_type", nullable = false, length = 20)
    private String providerType;

    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @ColumnDefault("1")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @ColumnDefault("1")
    @Column(name = "priority", nullable = false)
    private Short priority;

    @OneToMany
    @JoinColumn(name = "provider_id")
    private Set<EodFixing> eodFixings = new LinkedHashSet<>();

    @OneToMany
    @JoinColumn(name = "provider_id")
    private Set<ExchangeRate> exchangeRates = new LinkedHashSet<>();

    @OneToMany
    @JoinColumn(name = "provider_id")
    private Set<ForwardRate> forwardRates = new LinkedHashSet<>();

}