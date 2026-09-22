package com.paddling.service;

import com.paddling.dto.MonthlySettlementDTO;
import com.paddling.dto.SettlementSegmentDTO;
import com.paddling.entity.Binding;
import com.paddling.entity.MonthlySettlement;
import com.paddling.entity.Rack;
import com.paddling.entity.SettlementSegment;
import com.paddling.entity.Team;
import com.paddling.enums.MileageRange;
import com.paddling.exception.BusinessException;
import com.paddling.exception.ResourceNotFoundException;
import com.paddling.exception.SettlementAlreadySealedException;
import com.paddling.repository.BindingRepository;
import com.paddling.repository.MonthlySettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 月度里程封账结算服务。
 *
 * 封账语义：
 * 1. 取队伍当月生效期间相交的每一段绑定（含已解绑段），逐段与结算月求交集；
 * 2. 每段：覆盖天数（含首尾）× 封账时刻支架的日里程配额 = 段贡献里程；
 * 3. 各段累加成月里程，对照队伍训练档位的月达标线判定达标；
 * 4. 单据头与全部明细整段快照落库，此后上游（绑定/支架/队伍）任何改动都不会回写本单；
 * 5. (team_id, period_month) 唯一索引保证并发封账只落一张单，重复请求 409 失败。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final MonthlySettlementRepository settlementRepository;
    private final BindingRepository bindingRepository;
    private final TeamService teamService;
    private final RackService rackService;

    /**
     * 为指定队伍、月份生成（封账）结算单。
     */
    @Transactional
    public MonthlySettlement sealSettlement(Long teamId, String periodMonthRaw) {
        YearMonth period = parsePeriod(periodMonthRaw);
        String periodMonth = period.toString();
        YearMonth currentMonth = YearMonth.now();
        if (period.isAfter(currentMonth)) {
            throw new BusinessException("不能为未来月份封账: " + periodMonth);
        }

        // 并发预检：绝大多数重复点击在这里被快速挡住并给出明确提示
        if (settlementRepository.existsByTeamIdAndPeriodMonth(teamId, periodMonth)) {
            throw new SettlementAlreadySealedException(
                    "队伍(ID=" + teamId + ") " + periodMonth + " 的结算单已封账，不能重复生成");
        }

        Team team = teamService.getById(teamId);
        MileageRange range = team.getTrainingMileage();
        LocalDate monthStart = period.atDay(1);
        LocalDate monthEnd = period.atEndOfMonth();

        // 当月每一段生效绑定（含中途解绑的段、月中换架前后两段）
        List<Binding> bindings = bindingRepository.findOverlappingByTeamId(teamId, monthStart, monthEnd);

        MonthlySettlement settlement = new MonthlySettlement();
        settlement.setTeamId(team.getId());
        settlement.setTeamName(team.getName());
        settlement.setPeriodMonth(periodMonth);
        settlement.setTrainingMileage(range.name());
        settlement.setTrainingMileageLabel(range.getLabel());
        settlement.setTargetMileage(range.getTargetMileage());

        BigDecimal totalMileage = BigDecimal.ZERO;
        int segmentNo = 0;
        List<SettlementSegment> segments = new ArrayList<>();

        for (Binding binding : bindings) {
            // 段在结算月内的有效切片
            LocalDate segStart = max(monthStart, binding.getStartDate());
            LocalDate bindingEnd = binding.getEndDate();
            LocalDate effectiveEnd = (bindingEnd == null) ? monthEnd : min(monthEnd, bindingEnd);
            if (segStart.isAfter(effectiveEnd)) {
                // 理论上被 SQL 的交集条件排除，防御性跳过
                continue;
            }

            // 配额在封账这一刻读取并快照；支架缺失则整单失败回滚（任意一段算错都算整单不通过）
            Rack rack = rackService.getById(binding.getRackId());
            BigDecimal quota = rack.getDailyMileageQuota() != null ? rack.getDailyMileageQuota() : BigDecimal.ZERO;

            int coveredDays = (int) java.time.temporal.ChronoUnit.DAYS.between(segStart, effectiveEnd) + 1;
            BigDecimal contributed = quota.multiply(BigDecimal.valueOf(coveredDays)).setScale(2, java.math.RoundingMode.HALF_UP);

            SettlementSegment segment = new SettlementSegment();
            segment.setSettlement(settlement);
            segment.setSegmentNo(++segmentNo);
            segment.setBindingId(binding.getId());
            segment.setRackId(rack.getId());
            segment.setRackCode(rack.getCode());
            segment.setRackMileageRange(rack.getMileageRange() != null ? rack.getMileageRange().name() : null);
            segment.setSegmentStart(segStart);
            segment.setSegmentEnd(effectiveEnd);
            segment.setCoveredDays(coveredDays);
            segment.setDailyMileageQuota(quota.setScale(2, java.math.RoundingMode.HALF_UP));
            segment.setContributedMileage(contributed);
            segment.setBindingStartDate(binding.getStartDate());
            segment.setBindingEndDate(binding.getEndDate());
            segments.add(segment);

            totalMileage = totalMileage.add(contributed);
        }

        settlement.setSegments(segments);
        settlement.setSegmentCount(segments.size());
        settlement.setTotalMileage(totalMileage);
        settlement.setQualified(totalMileage.compareTo(range.getTargetMileage()) >= 0);

        try {
            // 明细级联一并落库；唯一索引 uk_team_period 是并发场景的最终防线
            return settlementRepository.saveAndFlush(settlement);
        } catch (DataIntegrityViolationException e) {
            // 两个人几乎同时点生成：后到事务在此撞唯一键，整单回滚并明确告知“已封账”
            log.warn("并发重复封账被唯一约束拦截: teamId={}, period={}", teamId, periodMonth);
            throw new SettlementAlreadySealedException(
                    "队伍(ID=" + teamId + ") " + periodMonth + " 的结算单已封账（并发请求已被拒绝），请勿重复生成");
        }
    }

    /** 查询已封结算单（含逐段明细）；未封账返回 404，由前端引导用户先封账 */
    @Transactional(readOnly = true)
    public MonthlySettlement getSealed(Long teamId, String periodMonthRaw) {
        YearMonth period = parsePeriod(periodMonthRaw);
        return settlementRepository.findByTeamIdAndPeriodMonth(teamId, period.toString())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "队伍(ID=" + teamId + ") " + period + " 尚无结算单，尚未封账"));
    }

    @Transactional(readOnly = true)
    public Optional<MonthlySettlement> findSealed(Long teamId, String periodMonthRaw) {
        YearMonth period = parsePeriod(periodMonthRaw);
        return settlementRepository.findByTeamIdAndPeriodMonth(teamId, period.toString());
    }

    @Transactional(readOnly = true)
    public Page<MonthlySettlement> list(Long teamId, Pageable pageable) {
        if (teamId != null) {
            return settlementRepository.findByTeamIdOrderByPeriodMonthDesc(teamId, pageable);
        }
        return settlementRepository.findAllByOrderByPeriodMonthDesc(pageable);
    }

    // ---------- DTO 组装（只读快照表，明细永远取冻结值，不回查上游） ----------

    @Transactional(readOnly = true)
    public MonthlySettlementDTO toDetailDTO(MonthlySettlement s) {
        List<SettlementSegmentDTO> segmentDTOs = s.getSegments().stream()
                .map(this::toSegmentDTO)
                .toList();
        MonthlySettlementDTO dto = toHeaderDTO(s);
        dto.setSegments(segmentDTOs);
        return dto;
    }

    public MonthlySettlementDTO toHeaderDTO(MonthlySettlement s) {
        MonthlySettlementDTO dto = new MonthlySettlementDTO();
        dto.setId(s.getId());
        dto.setTeamId(s.getTeamId());
        dto.setTeamName(s.getTeamName());
        dto.setPeriodMonth(s.getPeriodMonth());
        dto.setTrainingMileage(s.getTrainingMileage());
        dto.setTrainingMileageLabel(s.getTrainingMileageLabel());
        dto.setTargetMileage(s.getTargetMileage());
        dto.setTotalMileage(s.getTotalMileage());
        dto.setSegmentCount(s.getSegmentCount());
        dto.setQualified(s.getQualified());
        dto.setSealedAt(s.getSealedAt());
        return dto;
    }

    private SettlementSegmentDTO toSegmentDTO(SettlementSegment seg) {
        SettlementSegmentDTO dto = new SettlementSegmentDTO();
        dto.setId(seg.getId());
        dto.setSegmentNo(seg.getSegmentNo());
        dto.setBindingId(seg.getBindingId());
        dto.setRackId(seg.getRackId());
        dto.setRackCode(seg.getRackCode());
        dto.setRackMileageRange(seg.getRackMileageRange());
        if (seg.getRackMileageRange() != null) {
            try {
                dto.setRackMileageRangeLabel(MileageRange.valueOf(seg.getRackMileageRange()).getLabel());
            } catch (IllegalArgumentException ignore) {
                dto.setRackMileageRangeLabel(seg.getRackMileageRange());
            }
        }
        dto.setSegmentStart(seg.getSegmentStart());
        dto.setSegmentEnd(seg.getSegmentEnd());
        dto.setCoveredDays(seg.getCoveredDays());
        dto.setDailyMileageQuota(seg.getDailyMileageQuota());
        dto.setContributedMileage(seg.getContributedMileage());
        dto.setBindingStartDate(seg.getBindingStartDate());
        dto.setBindingEndDate(seg.getBindingEndDate());
        return dto;
    }

    private YearMonth parsePeriod(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("结算月份不能为空，格式 yyyy-MM");
        }
        try {
            return YearMonth.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("结算月份格式错误: " + raw + "，正确格式为 yyyy-MM（如 2026-08）");
        }
    }

    private LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
