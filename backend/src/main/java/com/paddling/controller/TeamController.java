package com.paddling.controller;

import com.paddling.common.Result;
import com.paddling.dto.TeamDTO;
import com.paddling.entity.Team;
import com.paddling.enums.MileageRange;
import com.paddling.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {
    
    private final TeamService teamService;
    
    @GetMapping
    public Result<Page<Team>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String trainingMileage) {
        
        MileageRange range = null;
        if (trainingMileage != null && !trainingMileage.isEmpty()) {
            range = MileageRange.valueOf(trainingMileage);
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Team> result = teamService.getList(range, pageable);
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<Team> getById(@PathVariable Long id) {
        Team team = teamService.getById(id);
        return Result.success(team);
    }
    
    @PostMapping
    public Result<Team> create(@RequestBody TeamDTO dto) {
        Team team = teamService.create(dto);
        return Result.success("创建成功", team);
    }
    
    @PutMapping("/{id}")
    public Result<Team> update(@PathVariable Long id, @RequestBody TeamDTO dto) {
        Team team = teamService.update(id, dto);
        return Result.success("更新成功", team);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        teamService.delete(id);
        return Result.success("删除成功", null);
    }
}