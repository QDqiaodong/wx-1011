package com.paddling.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 月度里程结算单（封账单）。
 *
 * 封账边界设计：
 * 1. 同一支队伍同一月份在 DB 层面有唯一约束 uk_team_period，并发重复封账时只有一个事务能插入成功，
 *    另一个事务落唯一键冲突并被转成“该队伍该月已封账”的业务失败。
 * 2. 单子里冗余并冻结队伍名称、训练档位、达标线等快照字段；不与 team/rack/binding 建任何外键关系，
 *    因此封账之后换绑、解绑、改支架配额、甚至删除上游数据，都不可能改动历史结算数字。
 * 3. 下一个月生成新单时重新读取当时的活数据计算，新月份自然反映上游变化——锁的只是“已生成的那一个月”。
 */
@Entity
@Table(name = "monthly_settlement",
        uniqueConstraints = @UniqueConstraint(name = "uk_team_period", columnNames = {"team_id", "period"}),
        indexes = {
                @Index(name = "idx_team_id", columnList = "team_id"),
                @Index(name = "idx_period", columnList = "period")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 封账月份，格式 yyyy-MM，与 teamId 共同唯一 */
    @Column(name = "period", nullable = false, length = 7)
    private String period;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    /** 快照：生成时刻的队伍名称，防止队伍改名后历史单据失真 */
    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    /** 快照：生成时刻队伍自己的训练里程档位 SHORT/MEDIUM/LONG */
    @Column(name = "training_mileage", nullable = false, length = 20)
    private String trainingMileage;

    /** 快照：该档位当月达标里程线(km) */
    @Column(name = "target_mileage", nullable = false, precision = 10, scale = 2)
    private BigDecimal targetMileage;

    /** 各绑定分段贡献里程之和(km) */
    @Column(name = "total_mileage", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalMileage;

    /** 当月所有生效绑定分段覆盖天数之和 */
    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    /** 达标分段数量（冗余汇总，明细以 settlement_segment 为准） */
    @Column(name = "segment_count", nullable = false)
    private Integer segmentCount;

    /** 是否达标：总里程 >= 达标线 */
    @Column(name = "qualified", nullable = false)
    private Boolean qualified;

    /** 账期起始日（含） */
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    /** 账期截止日（含） */
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    /** 封账时间，即快照生成时刻 */
    @Column(name = "sealed_at", updatable = false)
    private LocalDateTime sealedAt;

    @PrePersist
    protected void onCreate() {
        sealedAt = LocalDateTime.now();
    }
}
