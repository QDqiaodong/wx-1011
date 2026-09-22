package com.paddling.service;

import com.paddling.entity.Binding;
import com.paddling.entity.MonthlySettlement;
import com.paddling.entity.Rack;
import com.paddling.entity.SettlementSegment;
import com.paddling.entity.Team;
import com.paddling.enums.MileageRange;
import com.paddling.exception.BusinessException;
import com.paddling.exception.SettlementAlreadySealedException;
import com.paddling.repository.BindingRepository;
import com.paddling.repository.MonthlySettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 封账核心计算的纯单测（不起 Spring 容器、不连数据库）：
 * 分段交集、含首尾天数、段贡献累加、达标判定、并发重复封账拦截。
 */
@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private MonthlySettlementRepository settlementRepository;
    @Mock
    private BindingRepository bindingRepository;
    @Mock
    private TeamService teamService;
    @Mock
    private RackService rackService;

    @InjectMocks
    private SettlementService settlementService;

    private Team mediumTeam;

    @BeforeEach
    void setUp() {
        mediumTeam = new Team();
        mediumTeam.setId(2L);
        mediumTeam.setName("第二训练队");
        mediumTeam.setTrainingMileage(MileageRange.MEDIUM); // 达标线 300km
    }

    private Rack rack(long id, String code, BigDecimal quota) {
        Rack r = new Rack();
        r.setId(id);
        r.setCode(code);
        r.setMileageRange(MileageRange.MEDIUM);
        r.setDailyMileageQuota(quota);
        return r;
    }

    private Binding binding(long id, long rackId, LocalDate start, LocalDate end) {
        Binding b = new Binding();
        b.setId(id);
        b.setRackId(rackId);
        b.setTeamId(2L);
        b.setStartDate(start);
        b.setEndDate(end);
        b.setStatus(end == null ? "ACTIVE" : "INACTIVE");
        return b;
    }

    /** 模拟 saveAndFlush：分配 id、回填段的关联（JPA 级联行为） */
    @SuppressWarnings("unchecked")
    private void stubSaveFlush() {
        when(settlementRepository.saveAndFlush(any(MonthlySettlement.class))).thenAnswer(inv -> {
            MonthlySettlement s = inv.getArgument(0);
            s.setId(99L);
            int no = 1;
            for (SettlementSegment seg : s.getSegments()) {
                seg.setId(100L + no);
                seg.setSegmentNo(no++);
            }
            return s;
        });
    }

    @Test
    void 月中换架_两段分别切片_天数含首尾且合计正确() {
        // 2026-08 有 31 天。旧支架 8/1~8/10（10天 × 8km），换架日 8/10，新支架 8/11~至今（21天 × 12km）
        List<Binding> bindings = new ArrayList<>(List.of(
                binding(10L, 4L, LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-10")),
                binding(11L, 5L, LocalDate.parse("2026-08-11"), null)
        ));
        when(settlementRepository.existsByTeamIdAndPeriodMonth(2L, "2026-08")).thenReturn(false);
        when(teamService.getById(2L)).thenReturn(mediumTeam);
        when(bindingRepository.findOverlappingByTeamId(eq(2L), any(), any())).thenReturn(bindings);
        when(rackService.getById(4L)).thenReturn(rack(4L, "RACK004", new BigDecimal("8.00")));
        when(rackService.getById(5L)).thenReturn(rack(5L, "RACK005", new BigDecimal("12.00")));
        stubSaveFlush();

        MonthlySettlement result = settlementService.sealSettlement(2L, "2026-08");

        assertThat(result.getSegments()).hasSize(2);
        SettlementSegment s1 = result.getSegments().get(0);
        assertThat(s1.getSegmentNo()).isEqualTo(1);
        assertThat(s1.getSegmentStart()).isEqualTo(LocalDate.parse("2026-08-01"));
        assertThat(s1.getSegmentEnd()).isEqualTo(LocalDate.parse("2026-08-10"));
        assertThat(s1.getCoveredDays()).isEqualTo(10);
        assertThat(s1.getContributedMileage()).isEqualByComparingTo("80.00");

        SettlementSegment s2 = result.getSegments().get(1);
        assertThat(s2.getSegmentStart()).isEqualTo(LocalDate.parse("2026-08-11"));
        assertThat(s2.getSegmentEnd()).isEqualTo(LocalDate.parse("2026-08-31"));
        assertThat(s2.getCoveredDays()).isEqualTo(21);
        assertThat(s2.getContributedMileage()).isEqualByComparingTo("252.00");

        assertThat(result.getSegmentCount()).isEqualTo(2);
        assertThat(result.getTotalMileage()).isEqualByComparingTo("332.00");
        assertThat(result.getQualified()).isTrue(); // 332 >= 300
        assertThat(result.getTargetMileage()).isEqualByComparingTo("300.00");

        // 快照字段
        assertThat(s1.getRackCode()).isEqualTo("RACK004");
        assertThat(s1.getDailyMileageQuota()).isEqualByComparingTo("8.00");
        assertThat(result.getTeamName()).isEqualTo("第二训练队");
    }

    @Test
    void 跨月绑定_只切当月部分_未达标判定为false() {
        // 绑定 2026-07-15 起仍生效，8 月切片 = 8/1~8/31（31天 × 9km = 279 < 300）
        List<Binding> bindings = new ArrayList<>(List.of(
                binding(20L, 6L, LocalDate.parse("2026-07-15"), null)
        ));
        when(settlementRepository.existsByTeamIdAndPeriodMonth(2L, "2026-08")).thenReturn(false);
        when(teamService.getById(2L)).thenReturn(mediumTeam);
        when(bindingRepository.findOverlappingByTeamId(eq(2L), any(), any())).thenReturn(bindings);
        when(rackService.getById(6L)).thenReturn(rack(6L, "RACK006", new BigDecimal("9.00")));
        stubSaveFlush();

        MonthlySettlement result = settlementService.sealSettlement(2L, "2026-08");

        assertThat(result.getSegments()).hasSize(1);
        assertThat(result.getSegments().get(0).getCoveredDays()).isEqualTo(31);
        assertThat(result.getTotalMileage()).isEqualByComparingTo("279.00");
        assertThat(result.getQualified()).isFalse();
    }

    @Test
    void 中途解绑_解绑日计入贡献() {
        // 8/1 绑、8/15 解绑：15 天 × 10km = 150
        List<Binding> bindings = new ArrayList<>(List.of(
                binding(30L, 7L, LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-15"))
        ));
        when(settlementRepository.existsByTeamIdAndPeriodMonth(2L, "2026-08")).thenReturn(false);
        when(teamService.getById(2L)).thenReturn(mediumTeam);
        when(bindingRepository.findOverlappingByTeamId(eq(2L), any(), any())).thenReturn(bindings);
        when(rackService.getById(7L)).thenReturn(rack(7L, "RACK007", new BigDecimal("10.00")));
        stubSaveFlush();

        MonthlySettlement result = settlementService.sealSettlement(2L, "2026-08");
        assertThat(result.getSegments().get(0).getCoveredDays()).isEqualTo(15);
        assertThat(result.getTotalMileage()).isEqualByComparingTo("150.00");
    }

    @Test
    void 无任何绑定段_可封零里程不达标单() {
        when(settlementRepository.existsByTeamIdAndPeriodMonth(2L, "2026-08")).thenReturn(false);
        when(teamService.getById(2L)).thenReturn(mediumTeam);
        when(bindingRepository.findOverlappingByTeamId(eq(2L), any(), any())).thenReturn(List.of());
        stubSaveFlush();

        MonthlySettlement result = settlementService.sealSettlement(2L, "2026-08");
        assertThat(result.getSegments()).isEmpty();
        assertThat(result.getSegmentCount()).isZero();
        assertThat(result.getTotalMileage()).isEqualByComparingTo("0.00");
        assertThat(result.getQualified()).isFalse();
    }

    @Test
    void 预检命中已封账_直接409且不落库() {
        when(settlementRepository.existsByTeamIdAndPeriodMonth(2L, "2026-08")).thenReturn(true);

        assertThatThrownBy(() -> settlementService.sealSettlement(2L, "2026-08"))
                .isInstanceOf(SettlementAlreadySealedException.class)
                .hasMessageContaining("已封账");
    }

    @Test
    void 两个请求同时通过预检_唯一索引拦截后者409() {
        when(settlementRepository.existsByTeamIdAndPeriodMonth(anyLong(), eq("2026-08"))).thenReturn(false);
        when(teamService.getById(2L)).thenReturn(mediumTeam);
        when(bindingRepository.findOverlappingByTeamId(anyLong(), any(), any())).thenReturn(List.of());
        when(settlementRepository.saveAndFlush(any(MonthlySettlement.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry '2-2026-08' for key 'uk_team_period'"));

        assertThatThrownBy(() -> settlementService.sealSettlement(2L, "2026-08"))
                .isInstanceOf(SettlementAlreadySealedException.class)
                .hasMessageContaining("并发");
    }

    @Test
    void 未来月份拒绝封账() {
        assertThatThrownBy(() -> settlementService.sealSettlement(2L, "2099-01"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未来月份");
    }

    @Test
    void 月份格式错误给400提示() {
        assertThatThrownBy(() -> settlementService.sealSettlement(2L, "2026/08"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("格式");
    }

    @Test
    void 查询未封账月份_抛404() {
        when(settlementRepository.findByTeamIdAndPeriodMonth(2L, "2026-08")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> settlementService.getSealed(2L, "2026-08"))
                .isInstanceOf(com.paddling.exception.ResourceNotFoundException.class);
    }
}
