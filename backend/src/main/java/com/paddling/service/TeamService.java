package com.paddling.service;

import com.paddling.dto.TeamDTO;
import com.paddling.entity.Team;
import com.paddling.enums.MileageRange;
import com.paddling.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {
    
    private final TeamRepository teamRepository;
    
    @Cacheable(value = "team", key = "#id")
    public Team getById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("队伍不存在: " + id));
    }
    
    @Cacheable(value = "teamList", key = "#mileageRange != null ? #mileageRange.name() : 'ALL'")
    public Page<Team> getList(MileageRange mileageRange, Pageable pageable) {
        if (mileageRange != null) {
            return teamRepository.findByTrainingMileage(mileageRange, pageable);
        }
        return teamRepository.findAll(pageable);
    }
    
    @Transactional
    @CacheEvict(value = {"team", "teamList"}, allEntries = true)
    public Team create(TeamDTO dto) {
        if (teamRepository.existsByName(dto.getName())) {
            throw new RuntimeException("队伍名称已存在: " + dto.getName());
        }
        
        Team team = new Team();
        team.setName(dto.getName());
        team.setMemberCount(dto.getMemberCount());
        team.setTrainingMileage(MileageRange.valueOf(dto.getTrainingMileage()));
        team.setDescription(dto.getDescription());
        
        return teamRepository.save(team);
    }
    
    @Transactional
    @CacheEvict(value = {"team", "teamList"}, allEntries = true)
    public Team update(Long id, TeamDTO dto) {
        Team team = getById(id);
        
        if (!team.getName().equals(dto.getName()) && teamRepository.existsByName(dto.getName())) {
            throw new RuntimeException("队伍名称已存在: " + dto.getName());
        }
        
        team.setName(dto.getName());
        team.setMemberCount(dto.getMemberCount());
        team.setTrainingMileage(MileageRange.valueOf(dto.getTrainingMileage()));
        team.setDescription(dto.getDescription());
        
        return teamRepository.save(team);
    }
    
    @Transactional
    @CacheEvict(value = {"team", "teamList"}, allEntries = true)
    public void delete(Long id) {
        if (!teamRepository.existsById(id)) {
            throw new RuntimeException("队伍不存在: " + id);
        }
        teamRepository.deleteById(id);
    }
}