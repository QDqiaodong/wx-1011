package com.paddling.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySettlementDTO {
    private Long id;
    private Long teamId;
    private String teamName;
    /** yyyy-MM */
    private String periodMonth;
    private String trainingMileage;
    private String trainingMileageLabel;
    private BigDecimal targetMileage;
    private BigDecimal totalMileage;
    private Integer segmentCount;
    private Boolean qualified;
    private LocalDateTime sealedAt;
    /** 列表接口可不填；详情接口返回逐段快照 */
    private List<SettlementSegmentDTO> segments;
}
