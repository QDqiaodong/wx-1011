package com.paddling.controller;

import com.paddling.common.Result;
import com.paddling.dto.RackDTO;
import com.paddling.entity.Rack;
import com.paddling.enums.MileageRange;
import com.paddling.service.RackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/racks")
@RequiredArgsConstructor
public class RackController {
    
    private final RackService rackService;
    
    @GetMapping
    public Result<Page<Rack>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String mileageRange) {
        
        MileageRange range = null;
        if (mileageRange != null && !mileageRange.isEmpty()) {
            range = MileageRange.valueOf(mileageRange);
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Rack> result = rackService.getList(range, pageable);
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<Rack> getById(@PathVariable Long id) {
        Rack rack = rackService.getById(id);
        return Result.success(rack);
    }
    
    @PostMapping
    public Result<Rack> create(@RequestBody RackDTO dto) {
        Rack rack = rackService.create(dto);
        return Result.success("创建成功", rack);
    }
    
    @PutMapping("/{id}")
    public Result<Rack> update(@PathVariable Long id, @RequestBody RackDTO dto) {
        Rack rack = rackService.update(id, dto);
        return Result.success("更新成功", rack);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        rackService.delete(id);
        return Result.success("删除成功", null);
    }
}