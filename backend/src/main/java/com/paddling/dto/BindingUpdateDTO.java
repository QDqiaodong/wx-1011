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
}