package com.paddling.enums;

import java.math.BigDecimal;

/**
 * 里程结算标准：队伍训练档位对应的月度达标里程，以及该档位支架的默认日里程配额。
 * 达标线在结算单生成时快照进单子，之后即使调整本标准也不影响历史结算单。
 */
public enum MileageStandard {

    SHORT(MileageRange.SHORT, new BigDecimal("5.00"), new BigDecimal("120.00")),
    MEDIUM(MileageRange.MEDIUM, new BigDecimal("12.00"), new BigDecimal("300.00")),
    LONG(MileageRange.LONG, new BigDecimal("25.00"), new BigDecimal("600.00"));

    private final MileageRange range;
    private final BigDecimal defaultDailyQuota;
    private final BigDecimal monthlyTarget;

    MileageStandard(MileageRange range, BigDecimal defaultDailyQuota, BigDecimal monthlyTarget) {
        this.range = range;
        this.defaultDailyQuota = defaultDailyQuota;
        this.monthlyTarget = monthlyTarget;
    }

    public MileageRange getRange() {
        return range;
    }

    public BigDecimal getDefaultDailyQuota() {
        return defaultDailyQuota;
    }

    public BigDecimal getMonthlyTarget() {
        return monthlyTarget;
    }

    public static MileageStandard of(MileageRange range) {
        for (MileageStandard standard : values()) {
            if (standard.range == range) {
                return standard;
            }
        }
        throw new IllegalArgumentException("未知的训练里程档位: " + range);
    }
}
