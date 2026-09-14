package com.paddling.controller;

import com.paddling.common.Result;
import com.paddling.dto.BindingCreateDTO;
import com.paddling.dto.BindingDTO;
import com.paddling.dto.BindingUpdateDTO;
import com.paddling.entity.Binding;
import com.paddling.entity.BindingHistory;
import com.paddling.service.BindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bindings")
@RequiredArgsConstructor
public class BindingController {
    
    private final BindingService bindingService;
    
    @GetMapping
    public Result<Map<String, Object>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long rackId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String status) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Binding> bindings = bindingService.getList(rackId, teamId, status, pageable);
        
        List<BindingDTO> dtoList = bindings.getContent().stream()
                .map(bindingService::toDTO)
                .collect(Collectors.toList());
        
        Map<String, Object> result = Map.of(
                "content", dtoList,
                "totalElements", bindings.getTotalElements(),
                "totalPages", bindings.getTotalPages(),
                "currentPage", bindings.getNumber()
        );
        
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<BindingDTO> getById(@PathVariable Long id) {
        Binding binding = bindingService.getById(id);
        BindingDTO dto = bindingService.toDTO(binding);
        return Result.success(dto);
    }
    
    @GetMapping("/{id}/history")
    public Result<List<BindingHistory>> getHistory(@PathVariable Long id) {
        List<BindingHistory> history = bindingService.getHistory(id);
        return Result.success(history);
    }
    
    @PostMapping
    public Result<BindingDTO> create(@RequestBody BindingCreateDTO dto) {
        Binding binding = bindingService.create(dto);
        BindingDTO result = bindingService.toDTO(binding);
        return Result.success("绑定成功", result);
    }
    
    @PutMapping("/{id}")
    public Result<BindingDTO> update(@PathVariable Long id, @RequestBody BindingUpdateDTO dto) {
        Binding binding = bindingService.update(id, dto);
        BindingDTO result = bindingService.toDTO(binding);
        return Result.success("更新成功", result);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bindingService.delete(id);
        return Result.success("解绑成功", null);
    }
}