package com.paddling.enums;

import java.math.BigDecimal;

public enum MileageRange {
    // 月达标里程（km）：队伍按自身训练档位判定当月是否达标，阈值在封账时一并快照进结算单
    SHORT("短距离", "0-5km", new BigDecimal("100.00")),
    MEDIUM("中距离", "5-20km", new BigDecimal("300.00")),
    LONG("长距离", "20km以上", new BigDecimal("600.00"));

    private final String label;
    private final String description;
    /** 该档位队伍的月训练里程达标线（km） */
    private final BigDecimal targetMileage;

    MileageRange(String label, String description, BigDecimal targetMileage) {
        this.label = label;
        this.description = description;
        this.targetMileage = targetMileage;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getTargetMileage() {
        return targetMileage;
    }
}
