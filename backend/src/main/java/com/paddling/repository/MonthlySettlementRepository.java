package com.paddling.repository;

import com.paddling.entity.MonthlySettlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlySettlementRepository extends JpaRepository<MonthlySettlement, Long> {

    /** 按队伍 + 账期定位唯一结算单，唯一约束 uk_team_period 保证至多一条 */
    Optional<MonthlySettlement> findByTeamIdAndPeriod(Long teamId, String period);

    boolean existsByTeamIdAndPeriod(Long teamId, String period);

    Page<MonthlySettlement> findByTeamIdOrderByPeriodDesc(Long teamId, Pageable pageable);

    List<MonthlySettlement> findByPeriodOrderByIdAsc(String period);

    Page<MonthlySettlement> findAllByOrderByPeriodDescIdDesc(Pageable pageable);
}
