package com.paddling.repository;

import com.paddling.entity.Binding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BindingRepository extends JpaRepository<Binding, Long> {
    Page<Binding> findByRackId(Long rackId, Pageable pageable);
    
    Page<Binding> findByTeamId(Long teamId, Pageable pageable);
    
    Page<Binding> findByStatus(String status, Pageable pageable);
    
    Optional<Binding> findByRackIdAndTeamIdAndStatus(Long rackId, Long teamId, String status);
    
    List<Binding> findByStatus(String status);
    
    long countByStatus(String status);
    
    @Query("SELECT b FROM Binding b WHERE b.status = 'ACTIVE'")
    List<Binding> findActiveBindings();
    
    @Query("SELECT COUNT(DISTINCT b.rackId) FROM Binding b WHERE b.status = 'ACTIVE' AND b.rackId IN (SELECT r.id FROM Rack r WHERE r.mileageRange = :range)")
    long countActiveRacksByMileageRange(@Param("range") com.paddling.enums.MileageRange range);

    /**
     * 查询某支队伍所有与给定账期 [periodStart, periodEnd] 有交叠的绑定，不区分状态：
     * 中途解绑的绑定 end_date 已落值，未解绑的生效中绑定 end_date 为 null（按账期末日处理）。
     * 交叠条件：开始日 <= 账期末日 且 (结束日为空 或 结束日 >= 账期首日)。
     */
    @Query("SELECT b FROM Binding b WHERE b.teamId = :teamId " +
            "AND b.startDate <= :periodEnd " +
            "AND (b.endDate IS NULL OR b.endDate >= :periodStart) " +
            "ORDER BY b.startDate ASC, b.id ASC")
    List<Binding> findOverlappingByTeamAndPeriod(@Param("teamId") Long teamId,
                                                 @Param("periodStart") LocalDate periodStart,
                                                 @Param("periodEnd") LocalDate periodEnd);
}