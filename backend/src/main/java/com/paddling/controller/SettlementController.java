package com.paddling.controller;

import com.paddling.common.Result;
import com.paddling.dto.MonthlySettlementDTO;
import com.paddling.dto.SettlementCreateDTO;
import com.paddling.entity.MonthlySettlement;
import com.paddling.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 月度里程结算单（按月封账）。
 */
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * 生成结算单（封账）。同一队伍同一月重复封账返回 409，message 为失败原因。
     */
    @PostMapping
    public Result<MonthlySettlementDTO> seal(@Valid @RequestBody SettlementCreateDTO dto) {
        MonthlySettlement settlement = settlementService.sealSettlement(dto.getTeamId(), dto.getPeriodMonth());
        return Result.success("封账成功", settlementService.toDetailDTO(settlement));
    }

    /**
     * 按队伍 + 月份查询已封结算单及逐段明细；未封账返回 404。
     */
    @GetMapping
    public Result<MonthlySettlementDTO> getByTeamAndMonth(@RequestParam Long teamId,
                                                          @RequestParam String month) {
        MonthlySettlement settlement = settlementService.getSealed(teamId, month);
        return Result.success(settlementService.toDetailDTO(settlement));
    }

    /**
     * 结算单列表（可按队伍筛选），只返回单据头；点选后再查逐段明细。
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(required = false) Long teamId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MonthlySettlement> result = settlementService.list(teamId, pageable);
        List<MonthlySettlementDTO> content = result.getContent().stream()
                .map(settlementService::toHeaderDTO)
                .toList();
        Map<String, Object> data = Map.of(
                "content", content,
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "currentPage", result.getNumber()
        );
        return Result.success(data);
    }
}
