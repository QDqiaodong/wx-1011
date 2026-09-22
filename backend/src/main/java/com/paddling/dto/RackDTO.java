package com.paddling.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RackDTO {
    private Long id;
    private String code;
    private BigDecimal capacity;
    private String mileageRange;
    /** 日里程配额（km/天） */
    private BigDecimal dailyMileageQuota;
    private String description;
}
