package com.paddling.service.settlement;

import com.paddling.entity.Binding;
import com.paddling.entity.Rack;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 单段绑定在某个账期内的结算计算（纯函数，无 IO，便于单测保证“任意一段算错都算整单不通过”）。
 */
public final class SettlementCalculator {

    private SettlementCalculator() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentResult {
        /** 该段绑定与账期的交叠起始日（含） */
        private LocalDate segmentStart;
        /** 该段绑定与账期的交叠截止日（含） */
        private LocalDate segmentEnd;
        /** 覆盖天数（含首尾） */
        private Integer days;
        /** 贡献里程 = 天数 × 支架日里程配额 */
        private BigDecimal contributedMileage;
    }

    /**
     * 计算一段绑定在账期 [periodStart, periodEnd] 内的覆盖区间和贡献里程。
     *
     * @param binding     绑定记录；endDate 为 null 表示至今仍生效，账期内按账期末日封口
     * @param rack        绑定所指向的支架（取其日里程配额）
     * @param periodStart 账期首日（含）
     * @param periodEnd   账期末日（含）
     */
    public static SegmentResult calculate(Binding binding, Rack rack,
                                          LocalDate periodStart, LocalDate periodEnd) {
        LocalDate effectiveEnd = binding.getEndDate() != null ? binding.getEndDate() : periodEnd;

        LocalDate segmentStart = binding.getStartDate().isAfter(periodStart) ? binding.getStartDate() : periodStart;
        LocalDate segmentEnd = effectiveEnd.isBefore(periodEnd) ? effectiveEnd : periodEnd;

        if (segmentStart.isAfter(segmentEnd)) {
            throw new IllegalStateException(
                    "绑定 " + binding.getId() + " 在账期 " + periodStart + "~" + periodEnd + " 内无有效交叠区间");
        }

        // 首尾两天都计为有效绑定日
        int days = Math.toIntExact(ChronoUnit.DAYS.between(segmentStart, segmentEnd)) + 1;

        BigDecimal quota = rack.getDailyQuota();
        if (quota == null || quota.signum() <= 0) {
            throw new IllegalStateException("支架 " + rack.getCode() + " 的日里程配额必须大于0");
        }

        BigDecimal mileage = quota.multiply(BigDecimal.valueOf(days))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        return new SegmentResult(segmentStart, segmentEnd, days, mileage);
    }
}
