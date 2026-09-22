package com.paddling.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 月度里程结算单 DTO：表头快照 + 分段明细。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDTO {
    private Long id;
    private String period;
    private Long teamId;
    private String teamName;
    private String trainingMileage;
    private String trainingMileageLabel;
    private BigDecimal targetMileage;
    private BigDecimal totalMileage;
    private Integer totalDays;
    private Integer segmentCount;
    private Boolean qualified;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDateTime sealedAt;
    private List<SettlementSegmentDTO> segments;
}
