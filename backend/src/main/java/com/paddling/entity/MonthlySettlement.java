package com.paddling.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 月度里程结算单（封账单）。
 *
 * 封账边界设计：结算单头与每一段绑定明细（{@link SettlementSegment}）都以快照形式落库，
 * 单据生成后不再读取 binding / rack / team 任何上游数据：
 *  - 改支架日里程配额、换绑、解绑只影响后续月份的重新计算，历史结算单永远保持生成那一刻的值；
 *  - 同一支队伍下一个月没有结算单，结算时会按当时最新的上游数据计算，从而反映变化。
 *
 * 同一支队伍同一个月只能存在一张单据：
 *  uk_team_period 唯一索引是防并发重复封账的最终屏障（DB 层），
 *  Service 内预检 + 捕获唯一键冲突给出明确的“已封账”失败提示。
 */
@Entity
@Table(name = "monthly_settlement", uniqueConstraints = {
        @UniqueConstraint(name = "uk_team_period", columnNames = {"team_id", "period_month"})
}, indexes = {
        @Index(name = "idx_period_month", columnList = "periodMonth"),
        @Index(name = "idx_team_id", columnList = "teamId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    /** 队伍名称快照 */
    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    /** 结算月份，格式 yyyy-MM（每月唯一） */
    @Column(name = "period_month", nullable = false, length = 7)
    private String periodMonth;

    /** 队伍训练档位快照：SHORT/MEDIUM/LONG */
    @Column(name = "training_mileage", nullable = false, length = 20)
    private String trainingMileage;

    @Column(name = "training_mileage_label", nullable = false, length = 20)
    private String trainingMileageLabel;

    /** 月达标里程阈值快照（km），来自封账时队伍档位的配置 */
    @Column(name = "target_mileage", nullable = false, precision = 10, scale = 2)
    private BigDecimal targetMileage;

    /** 当月各绑定段贡献里程合计（km） */
    @Column(name = "total_mileage", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalMileage;

    /** 当月生效绑定段数 */
    @Column(name = "segment_count", nullable = false)
    private Integer segmentCount;

    /** 是否达标：totalMileage >= targetMileage */
    @Column(name = "qualified", nullable = false)
    private Boolean qualified;

    /** 封账时间（生成时刻，之后单据冻结不可变） */
    @Column(name = "sealed_at", updatable = false)
    private LocalDateTime sealedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 分段明细快照，按段序号升序；随结算单整单一起写入/读取 */
    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("segmentNo ASC")
    private List<SettlementSegment> segments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (sealedAt == null) {
            sealedAt = createdAt;
        }
    }

    /** 便于业务层取月起止日期 */
    public LocalDate periodStart() {
        return java.time.YearMonth.parse(periodMonth).atDay(1);
    }

    public LocalDate periodEnd() {
        return java.time.YearMonth.parse(periodMonth).atEndOfMonth();
    }
}
