package com.paddling.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 月度结算单分段明细：一支队伍当月处于生效绑定期间的“每一段绑定”一行。
 *
 * 每一行都把生成时刻的计价要素整段快照下来（绑定、支架、日里程配额、交叠起止、天数、贡献里程），
 * 不对外键做物理约束：事后改绑定起止、解绑、换绑或修改支架日配额，都不会回写本行，
 * 这是“旧单在上游数据变更后保持不变”的根本保证。
 */
@Entity
@Table(name = "settlement_segment",
        indexes = @Index(name = "idx_settlement_id", columnList = "settlement_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    /** 源绑定ID（仅作溯源，不建外键，绑定被删/改也不影响快照） */
    @Column(name = "binding_id", nullable = false)
    private Long bindingId;

    @Column(name = "rack_id", nullable = false)
    private Long rackId;

    /** 快照：生成时刻的支架编号 */
    @Column(name = "rack_code", nullable = false, length = 50)
    private String rackCode;

    /** 快照：支架适配里程区间 */
    @Column(name = "rack_mileage_range", length = 20)
    private String rackMileageRange;

    /** 快照：生成时刻该支架的日里程配额(km/天)——最关键的封账计价基数 */
    @Column(name = "daily_quota", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyQuota;

    /** 该段绑定与封账月份的交叠起始日（含） */
    @Column(name = "segment_start", nullable = false)
    private LocalDate segmentStart;

    /** 该段绑定与封账月份的交叠截止日（含） */
    @Column(name = "segment_end", nullable = false)
    private LocalDate segmentEnd;

    /** 交叠天数（含首尾，按自然日计） */
    @Column(name = "days", nullable = false)
    private Integer days;

    /** 贡献里程 = days × dailyQuota，生成时算死落库 */
    @Column(name = "contributed_mileage", nullable = false, precision = 12, scale = 2)
    private BigDecimal contributedMileage;

    /** 段内排序：按起始日、绑定ID，保证明细稳定呈现 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
