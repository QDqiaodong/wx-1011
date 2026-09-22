package com.paddling.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 封账请求：指定队伍与账期。
 * month 格式为 yyyy-MM（例如 2024-02），允许补封任意历史月份，不限于当月。
 */
@Data
public class SettlementCreateDTO {

    @NotNull(message = "队伍ID不能为空")
    private Long teamId;

    @NotNull(message = "封账月份不能为空，格式 yyyy-MM")
    private String month;
}
