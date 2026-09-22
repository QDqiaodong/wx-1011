package com.paddling.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paddling.entity.Binding;
import com.paddling.entity.MonthlySettlement;
import com.paddling.entity.Rack;
import com.paddling.entity.SettlementSegment;
import com.paddling.entity.Team;
import com.paddling.enums.MileageRange;
import com.paddling.repository.BindingRepository;
import com.paddling.repository.MonthlySettlementRepository;
import com.paddling.repository.RackRepository;
import com.paddling.repository.SettlementSegmentRepository;
import com.paddling.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端到端封账集成测试（H2 MySQL 模式 + MockMvc）：
 *  - 真实 JPA 映射/唯一索引、分段计算、达标判定
 *  - 409 重复封账（含并发）只落一张单
 *  - 封账后改上游（配额/解绑）旧单不变；新月份按新数据计算
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SettlementIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TeamRepository teamRepository;
    @Autowired private RackRepository rackRepository;
    @Autowired private BindingRepository bindingRepository;
    @Autowired private MonthlySettlementRepository settlementRepository;
    @Autowired private SettlementSegmentRepository segmentRepository;
    @Autowired private BindingService bindingService;

    private Team longTeam;
    private Rack rackA;
    private Rack rackB;
    private final YearMonth prevMonth = YearMonth.now().minusMonths(1);
    private final YearMonth thisMonth = YearMonth.now();

    @BeforeEach
    void setUp() {
        segmentRepository.deleteAll();
        settlementRepository.deleteAll();
        bindingRepository.deleteAll();
        rackRepository.deleteAll();
        teamRepository.deleteAll();

        longTeam = new Team();
        longTeam.setName("长距离测试队");
        longTeam.setMemberCount(10);
        longTeam.setTrainingMileage(MileageRange.LONG); // 达标线 600km
        longTeam = teamRepository.save(longTeam);

        rackA = new Rack();
        rackA.setCode("RA-A");
        rackA.setCapacity(new BigDecimal("400"));
        rackA.setMileageRange(MileageRange.LONG);
        rackA.setDailyMileageQuota(new BigDecimal("20.00"));
        rackA = rackRepository.save(rackA);

        rackB = new Rack();
        rackB.setCode("RA-B");
        rackB.setCapacity(new BigDecimal("420"));
        rackB.setMileageRange(MileageRange.LONG);
        rackB.setDailyMileageQuota(new BigDecimal("30.00"));
        rackB = rackRepository.save(rackB);
    }

    private Binding persistBinding(Long rackId, LocalDate start, LocalDate end, String status) {
        Binding b = new Binding();
        b.setRackId(rackId);
        b.setTeamId(longTeam.getId());
        b.setStartDate(start);
        b.setEndDate(end);
        b.setStatus(status);
        return bindingRepository.save(b);
    }

    private MvcResult sealViaHttp(Long teamId, String month) throws Exception {
        return mockMvc.perform(post("/api/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("teamId", teamId, "periodMonth", month))))
                .andReturn();
    }

    @Test
    void 换架分段封账_HTTP返回达标结论与逐段明细() throws Exception {
        // 上月：RA-A 用到月中 15 日，16 日起换 RA-B
        LocalDate s1 = prevMonth.atDay(1);
        LocalDate e1 = prevMonth.atDay(15);
        LocalDate s2 = prevMonth.atDay(16);
        persistBinding(rackA.getId(), s1, e1, "INACTIVE");
        persistBinding(rackB.getId(), s2, null, "ACTIVE");

        MvcResult result = sealViaHttp(longTeam.getId(), prevMonth.toString());
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = body.path("data");

        assertThat(data.path("periodMonth").asText()).isEqualTo(prevMonth.toString());
        assertThat(data.path("segmentCount").asInt()).isEqualTo(2);
        assertThat(data.path("targetMileage").asDouble()).isEqualTo(600.0);

        int daysA = 15;
        int daysB = prevMonth.lengthOfMonth() - 15;
        double expected = daysA * 20.0 + daysB * 30.0;
        assertThat(data.path("totalMileage").asDouble()).isEqualTo(expected);
        assertThat(data.path("qualified").asBoolean()).isEqualTo(expected >= 600.0);

        JsonNode segs = data.path("segments");
        assertThat(segs.get(0).path("rackCode").asText()).isEqualTo("RA-A");
        assertThat(segs.get(0).path("coveredDays").asInt()).isEqualTo(daysA);
        assertThat(segs.get(0).path("contributedMileage").asDouble()).isEqualTo(daysA * 20.0);
        assertThat(segs.get(0).path("segmentEnd").asText()).isEqualTo(e1.toString());
        assertThat(segs.get(1).path("rackCode").asText()).isEqualTo("RA-B");
        assertThat(segs.get(1).path("coveredDays").asInt()).isEqualTo(daysB);
        assertThat(segs.get(1).path("contributedMileage").asDouble()).isEqualTo(daysB * 30.0);
    }

    @Test
    void 重复封账_HTTP409且只保留一张单() throws Exception {
        persistBinding(rackA.getId(), prevMonth.atDay(1), null, "ACTIVE");

        assertThat(sealViaHttp(longTeam.getId(), prevMonth.toString()).getResponse().getStatus()).isEqualTo(200);

        MvcResult second = sealViaHttp(longTeam.getId(), prevMonth.toString());
        assertThat(second.getResponse().getStatus()).isEqualTo(409);
        JsonNode body = objectMapper.readTree(second.getResponse().getContentAsString());
        assertThat(body.path("code").asInt()).isEqualTo(409);
        assertThat(body.path("message").asText()).contains("已封账");
        assertThat(body.path("data").isNull());

        assertThat(settlementRepository.findAll()).hasSize(1);
    }

    @Test
    void 八人并发封账_最终只落一张单_其余409() throws Exception {
        persistBinding(rackA.getId(), prevMonth.atDay(1), null, "ACTIVE");

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        List<String> messages = java.util.Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    MvcResult r = sealViaHttp(longTeam.getId(), prevMonth.toString());
                    if (r.getResponse().getStatus() == 200) {
                        ok.incrementAndGet();
                    } else if (r.getResponse().getStatus() == 409) {
                        conflict.incrementAndGet();
                        JsonNode node = objectMapper.readTree(r.getResponse().getContentAsString());
                        messages.add(node.path("message").asText());
                    }
                } catch (Exception e) {
                    messages.add("EX:" + e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(ok.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(threads - 1);
        assertThat(settlementRepository.count()).isEqualTo(1);
        assertThat(segmentRepository.count()).isEqualTo(1L); // 金额没有被算两遍
        assertThat(messages).allSatisfy(m -> assertThat(m).contains("已封账"));
    }

    @Test
    void 封账后改配额加解绑_旧单不变_当月按新数据重算() throws Exception {
        // 上个月整月 RA-B（30km/天）
        persistBinding(rackB.getId(), prevMonth.atDay(1), null, "ACTIVE");

        sealViaHttp(longTeam.getId(), prevMonth.toString());
        MonthlySettlement oldBill = settlementRepository
                .findByTeamIdAndPeriodMonth(longTeam.getId(), prevMonth.toString()).orElseThrow();
        BigDecimal oldTotal = oldBill.getTotalMileage();
        List<SettlementSegment> oldBillSegments =
                segmentRepository.findBySettlement_IdOrderBySegmentNoAsc(oldBill.getId());
        BigDecimal oldQuotaSnapshot = oldBillSegments.get(0).getDailyMileageQuota();
        BigDecimal oldContributedSnapshot = oldBillSegments.get(0).getContributedMileage();
        assertThat(oldTotal).isEqualByComparingTo(
                new BigDecimal(30).multiply(BigDecimal.valueOf(prevMonth.lengthOfMonth())));
        assertThat(oldBill.getQualified()).isTrue();

        // —— 封账之后上游发生变化 ——
        // 1) 改支架配额（旧单快照不受影响）
        rackB.setDailyMileageQuota(new BigDecimal("99.99"));
        rackRepository.save(rackB);
        // 2) 当月 3 日通过业务接口换绑到 RA-A：旧段截止 3 日、新段 4 日起生效
        Binding active = bindingRepository.findAll().get(0);
        com.paddling.dto.BindingUpdateDTO updateDTO = new com.paddling.dto.BindingUpdateDTO();
        updateDTO.setRackId(rackA.getId());
        updateDTO.setChangeReason("封账后调整计划");
        updateDTO.setChangeDate(thisMonth.atDay(3).toString());
        bindingService.update(active.getId(), updateDTO);

        // 旧单再查一次：数值与快照完全不变（头 + 明细）
        mockMvc.perform(get("/api/settlements")
                        .param("teamId", String.valueOf(longTeam.getId()))
                        .param("month", prevMonth.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMileage").value(oldTotal.doubleValue()))
                .andExpect(jsonPath("$.data.qualified").value(true))
                .andExpect(jsonPath("$.data.segments[0].dailyMileageQuota").value(oldQuotaSnapshot.doubleValue()))
                .andExpect(jsonPath("$.data.segments[0].contributedMileage")
                        .value(oldContributedSnapshot.doubleValue()));

        // DB 层直接确认明细行未被动过
        List<SettlementSegment> oldSegments = segmentRepository.findBySettlement_IdOrderBySegmentNoAsc(oldBill.getId());
        assertThat(oldSegments).hasSize(1);
        assertThat(oldSegments.get(0).getDailyMileageQuota()).isEqualByComparingTo("30.00");
        assertThat(oldSegments.get(0).getContributedMileage()).isEqualByComparingTo(oldTotal);

        // 当月封账：两段都按“新数据”计算 —— 旧段 1~3 日用改后的 99.99，新段 4 日起 RA-A 用 20
        MvcResult thisResult = sealViaHttp(longTeam.getId(), thisMonth.toString());
        assertThat(thisResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode thisData = objectMapper.readTree(thisResult.getResponse().getContentAsString()).path("data");
        assertThat(thisData.path("id").asLong()).isNotEqualTo(oldBill.getId());
        assertThat(thisData.path("periodMonth").asText()).isEqualTo(thisMonth.toString());

        JsonNode thisSegs = thisData.path("segments");
        assertThat(thisSegs.size()).isEqualTo(2);
        assertThat(thisSegs.get(0).path("rackCode").asText()).isEqualTo("RA-B");
        assertThat(thisSegs.get(0).path("coveredDays").asInt()).isEqualTo(3);
        assertThat(thisSegs.get(0).path("dailyMileageQuota").asDouble()).isEqualTo(99.99);
        assertThat(thisSegs.get(0).path("contributedMileage").asDouble())
                .isEqualTo(99.99 * 3, org.assertj.core.data.Offset.offset(0.01));
        assertThat(thisSegs.get(1).path("rackCode").asText()).isEqualTo("RA-A");
        assertThat(thisSegs.get(1).path("segmentStart").asText()).isEqualTo(thisMonth.atDay(4).toString());
        assertThat(thisSegs.get(1).path("coveredDays").asInt()).isEqualTo(thisMonth.lengthOfMonth() - 3);
        assertThat(thisSegs.get(1).path("dailyMileageQuota").asDouble()).isEqualTo(20.0);

        double recomputedTotal = 0;
        for (JsonNode seg : thisSegs) {
            recomputedTotal += seg.path("contributedMileage").asDouble();
            // 每段：贡献 = 天数 × 配额，逐段自洽
            assertThat(seg.path("contributedMileage").asDouble())
                    .isEqualTo(seg.path("coveredDays").asInt() * seg.path("dailyMileageQuota").asDouble(),
                            org.assertj.core.data.Offset.offset(0.01));
        }
        assertThat(recomputedTotal).isEqualTo(thisData.path("totalMileage").asDouble(),
                org.assertj.core.data.Offset.offset(0.01));
        // 两张单并存，互不影响
        assertThat(settlementRepository.count()).isEqualTo(2);
    }

    @Test
    void 查询未封账月份_404() throws Exception {
        mockMvc.perform(get("/api/settlements")
                        .param("teamId", String.valueOf(longTeam.getId()))
                        .param("month", prevMonth.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("尚未封账")));
    }
}
