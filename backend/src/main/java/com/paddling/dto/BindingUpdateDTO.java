package com.paddling.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BindingUpdateDTO {
    private Long rackId;
    private String changeReason;
    private String operator = "system";
    /** 换绑日期（yyyy-MM-dd），默认今天；旧绑定计到当日，新绑定次日生效 */
    private String changeDate;
}
