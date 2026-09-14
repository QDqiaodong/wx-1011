package com.paddling.service;

import com.paddling.dto.BindingCreateDTO;
import com.paddling.dto.BindingDTO;
import com.paddling.dto.BindingUpdateDTO;
import com.paddling.entity.Binding;
import com.paddling.entity.BindingHistory;
import com.paddling.entity.Rack;
import com.paddling.entity.Team;
import com.paddling.repository.BindingHistoryRepository;
import com.paddling.repository.BindingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BindingService {
    
    private final BindingRepository bindingRepository;
    private final BindingHistoryRepository bindingHistoryRepository;
    private final RackService rackService;
    private final TeamService teamService;
    
    @Cacheable(value = "binding", key = "#id")
    public Binding getById(Long id) {
        return bindingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("绑定关系不存在: " + id));
    }
    
    public Page<Binding> getList(Long rackId, Long teamId, String status, Pageable pageable) {
        if (rackId != null) {
            return bindingRepository.findByRackId(rackId, pageable);
        }
        if (teamId != null) {
            return bindingRepository.findByTeamId(teamId, pageable);
        }
        if (status != null) {
            return bindingRepository.findByStatus(status, pageable);
        }
        return bindingRepository.findAll(pageable);
    }
    
    public List<BindingHistory> getHistory(Long bindingId) {
        return bindingHistoryRepository.findByBindingIdOrderByChangedAtDesc(bindingId);
    }
    
    @Transactional
    @CacheEvict(value = {"binding", "statistics"}, allEntries = true)
    public Binding create(BindingCreateDTO dto) {
        Rack rack = rackService.getById(dto.getRackId());
        Team team = teamService.getById(dto.getTeamId());
        
        if (bindingRepository.findByRackIdAndTeamIdAndStatus(dto.getRackId(), dto.getTeamId(), "ACTIVE").isPresent()) {
            throw new RuntimeException("该队伍已绑定该支架");
        }
        
        Binding binding = new Binding();
        binding.setRackId(dto.getRackId());
        binding.setTeamId(dto.getTeamId());
        binding.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now());
        binding.setStatus("ACTIVE");
        
        Binding saved = bindingRepository.save(binding);
        
        BindingHistory history = new BindingHistory();
        history.setBindingId(saved.getId());
        history.setNewRackId(dto.getRackId());
        history.setChangeReason("初始绑定");
        bindingHistoryRepository.save(history);
        
        return saved;
    }
    
    @Transactional
    @CacheEvict(value = {"binding", "statistics"}, allEntries = true)
    public Binding update(Long id, BindingUpdateDTO dto) {
        Binding binding = getById(id);
        
        if (!binding.getStatus().equals("ACTIVE")) {
            throw new RuntimeException("绑定关系已失效");
        }
        
        Long oldRackId = binding.getRackId();
        
        if (!oldRackId.equals(dto.getRackId())) {
            rackService.getById(dto.getRackId());
            
            BindingHistory history = new BindingHistory();
            history.setBindingId(id);
            history.setOldRackId(oldRackId);
            history.setNewRackId(dto.getRackId());
            history.setChangeReason(dto.getChangeReason());
            history.setOperator(dto.getOperator() != null ? dto.getOperator() : "system");
            bindingHistoryRepository.save(history);
            
            binding.setRackId(dto.getRackId());
        }
        
        return bindingRepository.save(binding);
    }
    
    @Transactional
    @CacheEvict(value = {"binding", "statistics"}, allEntries = true)
    public void delete(Long id) {
        Binding binding = getById(id);
        binding.setStatus("INACTIVE");
        binding.setEndDate(LocalDate.now());
        bindingRepository.save(binding);
    }
    
    public BindingDTO toDTO(Binding binding) {
        BindingDTO dto = new BindingDTO();
        dto.setId(binding.getId());
        dto.setRackId(binding.getRackId());
        dto.setTeamId(binding.getTeamId());
        dto.setStartDate(binding.getStartDate());
        dto.setEndDate(binding.getEndDate());
        dto.setStatus(binding.getStatus());
        
        try {
            Rack rack = rackService.getById(binding.getRackId());
            dto.setRackCode(rack.getCode());
            dto.setRackMileageRange(rack.getMileageRange().name());
        } catch (Exception e) {
            dto.setRackCode("未知");
            dto.setRackMileageRange("未知");
        }
        
        try {
            Team team = teamService.getById(binding.getTeamId());
            dto.setTeamName(team.getName());
            dto.setTeamMileageRange(team.getTrainingMileage().name());
        } catch (Exception e) {
            dto.setTeamName("未知");
            dto.setTeamMileageRange("未知");
        }
        
        return dto;
    }
}