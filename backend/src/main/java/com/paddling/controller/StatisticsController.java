package com.paddling.controller;

import com.paddling.common.Result;
import com.paddling.dto.StatisticsDTO;
import com.paddling.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    @GetMapping("/mileage")
    public Result<StatisticsDTO> getMileageStatistics() {
        StatisticsDTO stats = statisticsService.getMileageStatistics();
        return Result.success(stats);
    }
    
    @GetMapping("/mileage/{range}")
    public Result<Map<String, Object>> getMileageDetail(@PathVariable String range) {
        Map<String, Object> detail = statisticsService.getMileageDetail(range);
        return Result.success(detail);
    }
    
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> summary = statisticsService.getDashboardSummary();
        return Result.success(summary);
    }
}