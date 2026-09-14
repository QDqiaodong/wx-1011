package com.paddling.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BindingDTO {
    private Long id;
    private Long rackId;
    private Long teamId;
    private String rackCode;
    private String teamName;
    private String rackMileageRange;
    private String teamMileageRange;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}