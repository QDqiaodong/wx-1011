package com.paddling.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 封账请求：队伍 + 月份（yyyy-MM）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementCreateDTO {
    @NotNull(message = "队伍ID不能为空")
    private Long teamId;

    @NotNull(message = "结算月份不能为空，格式 yyyy-MM")
    private String periodMonth;
}
