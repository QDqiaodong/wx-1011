package com.paddling.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 月度结算单分段明细 DTO（全部为封账时刻的快照值）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementSegmentDTO {
    private Long id;
    private Long settlementId;
    private Long bindingId;
    private Long rackId;
    private String rackCode;
    private String rackMileageRange;
    private String rackMileageLabel;
    private BigDecimal dailyQuota;
    private LocalDate segmentStart;
    private LocalDate segmentEnd;
    private Integer days;
    private BigDecimal contributedMileage;
    private Integer sortOrder;
}
