package com.paddling.repository;

import com.paddling.entity.MonthlySettlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlySettlementRepository extends JpaRepository<MonthlySettlement, Long> {

    /**
     * 按队伍 + 月份定位结算单。并发封账时 Service 预检依赖它；
     * 真正的唯一性由 uk_team_period(team_id, period_month) 唯一索引兜底。
     */
    Optional<MonthlySettlement> findByTeamIdAndPeriodMonth(Long teamId, String periodMonth);

    boolean existsByTeamIdAndPeriodMonth(Long teamId, String periodMonth);

    Page<MonthlySettlement> findByTeamIdOrderByPeriodMonthDesc(Long teamId, Pageable pageable);

    Page<MonthlySettlement> findAllByOrderByPeriodMonthDesc(Pageable pageable);
}
