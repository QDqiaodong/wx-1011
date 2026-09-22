package com.paddling.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算单中的一段绑定明细（整段快照）。
 *
 * 一行 = 该队伍当月处于生效期间的“一段绑定”与结算月相交后的切片：
 * 覆盖天数（含首尾）× 封账那一刻支架的日里程配额 = 该段贡献里程。
 *
 * 所有字段（含支架编号、配额）都在封账时拷贝自上游表，之后即使解绑、换绑、
 * 修改支架配额，本行数值也不会变化 —— 这是旧单在上游数据变更后保持不变的原因。
 */
@Entity
@Table(name = "settlement_segment", indexes = {
        @Index(name = "idx_settlement_id", columnList = "settlement_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属结算单（随单头级联写入，明细不可脱离结算单单独存在/修改） */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private MonthlySettlement settlement;

    /** 段序号，从 1 开始 */
    @Column(name = "segment_no", nullable = false)
    private Integer segmentNo;

    /** 来源绑定ID快照（仅作溯源，不做外键关联，上游改动不影响本行） */
    @Column(name = "binding_id")
    private Long bindingId;

    @Column(name = "rack_id", nullable = false)
    private Long rackId;

    /** 支架编号快照 */
    @Column(name = "rack_code", nullable = false, length = 50)
    private String rackCode;

    /** 支架适配里程区间快照 */
    @Column(name = "rack_mileage_range", length = 20)
    private String rackMileageRange;

    /** 该绑定在结算月内生效切片的起始日 */
    @Column(name = "segment_start", nullable = false)
    private LocalDate segmentStart;

    /** 该绑定在结算月内生效切片的结束日 */
    @Column(name = "segment_end", nullable = false)
    private LocalDate segmentEnd;

    /** 切片覆盖天数（含首尾） */
    @Column(name = "covered_days", nullable = false)
    private Integer coveredDays;

    /** 日里程配额快照（km/天），取自封账时刻的支架配置 */
    @Column(name = "daily_mileage_quota", nullable = false, precision = 8, scale = 2)
    private BigDecimal dailyMileageQuota;

    /** 该段贡献里程 = coveredDays × dailyMileageQuota（km） */
    @Column(name = "contributed_mileage", nullable = false, precision = 12, scale = 2)
    private BigDecimal contributedMileage;

    /** 绑定原始开始/结束日快照，便于展示该段的来历（解绑单的结束日即解绑日） */
    @Column(name = "binding_start_date", nullable = false)
    private LocalDate bindingStartDate;

    @Column(name = "binding_end_date")
    private LocalDate bindingEndDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
