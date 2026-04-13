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
@Table(name = "rate_alert")
public class RateAlert implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_id", nullable = false)
    private CurrencyPair pair;

    @Column(name = "alert_type", nullable = false, length = 20)
    private String alertType;

    @Column(name = "threshold_value", precision = 18, scale = 6)
    private BigDecimal thresholdValue;

    @Column(name = "actual_value", precision = 18, scale = 6)
    private BigDecimal actualValue;

    @Column(name = "alert_message", nullable = false)
    private String alertMessage;

    @Column(name = "severity", nullable = false, length = 10)
    private String severity;

    @ColumnDefault("current_timestamp()")
    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "acknowledged_by", length = 50)
    private String acknowledgedBy;

    @ColumnDefault("'OPEN'")
    @Column(name = "status", nullable = false, length = 20)
    private String status;

}