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
@Table(name = "rate_audit_log")
public class RateAuditLog implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id", nullable = false)
    private Integer id;

    @Column(name = "rate_id", nullable = false)
    private Integer rateId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_id", nullable = false)
    private CurrencyPair pair;

    @Column(name = "action", nullable = false, length = 10)
    private String action;

    @Column(name = "old_mid_rate", precision = 18, scale = 6)
    private BigDecimal oldMidRate;

    @Column(name = "new_mid_rate", precision = 18, scale = 6)
    private BigDecimal newMidRate;

    @Column(name = "change_pct", precision = 8, scale = 4)
    private BigDecimal changePct;

    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;

    @ColumnDefault("current_timestamp()")
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "reason", length = 120)
    private String reason;

}