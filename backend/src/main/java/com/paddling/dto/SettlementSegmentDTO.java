package com.paddling.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementSegmentDTO {
    private Long id;
    private Integer segmentNo;
    private Long bindingId;
    private Long rackId;
    private String rackCode;
    private String rackMileageRange;
    private String rackMileageRangeLabel;
    /** 该段在结算月内的生效起止（切片） */
    private LocalDate segmentStart;
    private LocalDate segmentEnd;
    private Integer coveredDays;
    /** 封账时刻冻结的日里程配额（km/天） */
    private BigDecimal dailyMileageQuota;
    /** 该段贡献里程（km） */
    private BigDecimal contributedMileage;
    /** 来源绑定的原始起止快照 */
    private LocalDate bindingStartDate;
    private LocalDate bindingEndDate;
}
