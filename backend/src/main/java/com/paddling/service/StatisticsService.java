package com.paddling.service;

import com.paddling.dto.StatisticsDTO;
import com.paddling.entity.Rack;
import com.paddling.enums.MileageRange;
import com.paddling.repository.BindingRepository;
import com.paddling.repository.RackRepository;
import com.paddling.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {
    
    private final RackRepository rackRepository;
    private final BindingRepository bindingRepository;
    private final TeamRepository teamRepository;
    
    @Cacheable(value = "statistics", key = "'mileage'")
    public StatisticsDTO getMileageStatistics() {
        Map<String, Long> mileageRackCount = new LinkedHashMap<>();
        
        for (MileageRange range : MileageRange.values()) {
            long count = rackRepository.countByMileageRange(range);
            mileageRackCount.put(range.name(), count);
        }
        
        StatisticsDTO dto = new StatisticsDTO();
        dto.setMileageRackCount(mileageRackCount);
        return dto;
    }
    
    @Cacheable(value = "statistics", key = "'mileageDetail:' + #range")
    public Map<String, Object> getMileageDetail(String range) {
        MileageRange mileageRange = MileageRange.valueOf(range);
        
        List<Rack> racks = rackRepository.findByMileageRange(mileageRange);
        long activeBindingCount = bindingRepository.countActiveRacksByMileageRange(mileageRange);
        
        Map<String, Object> result = new HashMap<>();
        result.put("range", range);
        result.put("rangeLabel", mileageRange.getLabel());
        result.put("rangeDescription", mileageRange.getDescription());
        result.put("totalRacks", racks.size());
        result.put("activeBindings", activeBindingCount);
        result.put("racks", racks);
        
        return result;
    }
    
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        
        long totalRacks = rackRepository.count();
        long activeBindings = bindingRepository.countByStatus("ACTIVE");
        long totalTeams = teamRepository.count();
        
        StatisticsDTO mileageStats = getMileageStatistics();
        
        summary.put("totalRacks", totalRacks);
        summary.put("activeBindings", activeBindings);
        summary.put("totalTeams", totalTeams);
        summary.put("mileageStats", mileageStats.getMileageRackCount());
        
        return summary;
    }
}