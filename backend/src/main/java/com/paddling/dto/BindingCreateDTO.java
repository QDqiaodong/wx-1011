package com.paddling.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BindingCreateDTO {
    private Long rackId;
    private Long teamId;
    private LocalDate startDate;
}