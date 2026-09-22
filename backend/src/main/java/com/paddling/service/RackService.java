package com.paddling.service;

import com.paddling.dto.RackDTO;
import com.paddling.entity.Rack;
import com.paddling.enums.MileageRange;
import com.paddling.repository.RackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RackService {
    
    private final RackRepository rackRepository;
    
    @Cacheable(value = "rack", key = "#id")
    public Rack getById(Long id) {
        return rackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("支架不存在: " + id));
    }
    
    @Cacheable(value = "rackList", key = "#mileageRange != null ? #mileageRange.name() : 'ALL'")
    public Page<Rack> getList(MileageRange mileageRange, Pageable pageable) {
        if (mileageRange != null) {
            return rackRepository.findByMileageRange(mileageRange, pageable);
        }
        return rackRepository.findAll(pageable);
    }
    
    @Cacheable(value = "rackListByMileage", key = "#range")
    public List<Rack> getByMileageRange(MileageRange range) {
        return rackRepository.findByMileageRange(range);
    }
    
    @Transactional
    @CacheEvict(value = {"rack", "rackList", "rackListByMileage", "statistics"}, allEntries = true)
    public Rack create(RackDTO dto) {
        if (rackRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("支架编号已存在: " + dto.getCode());
        }
        
        Rack rack = new Rack();
        rack.setCode(dto.getCode());
        rack.setCapacity(dto.getCapacity());
        rack.setMileageRange(MileageRange.valueOf(dto.getMileageRange()));
        rack.setDailyMileageQuota(dto.getDailyMileageQuota() != null ? dto.getDailyMileageQuota() : java.math.BigDecimal.ZERO);
        rack.setDescription(dto.getDescription());

        return rackRepository.save(rack);
    }
    
    @Transactional
    @CacheEvict(value = {"rack", "rackList", "rackListByMileage", "statistics"}, allEntries = true)
    public Rack update(Long id, RackDTO dto) {
        Rack rack = getById(id);
        
        if (!rack.getCode().equals(dto.getCode()) && rackRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("支架编号已存在: " + dto.getCode());
        }
        
        rack.setCode(dto.getCode());
        rack.setCapacity(dto.getCapacity());
        rack.setMileageRange(MileageRange.valueOf(dto.getMileageRange()));
        rack.setDailyMileageQuota(dto.getDailyMileageQuota() != null ? dto.getDailyMileageQuota() : java.math.BigDecimal.ZERO);
        rack.setDescription(dto.getDescription());

        return rackRepository.save(rack);
    }
    
    @Transactional
    @CacheEvict(value = {"rack", "rackList", "rackListByMileage", "statistics"}, allEntries = true)
    public void delete(Long id) {
        if (!rackRepository.existsById(id)) {
            throw new RuntimeException("支架不存在: " + id);
        }
        rackRepository.deleteById(id);
    }
    
    public long countByMileageRange(MileageRange range) {
        return rackRepository.countByMileageRange(range);
    }
}