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
     * 查询某支队伍生效期间与 [rangeStart, rangeEnd] 有交集的全部绑定段（含已解绑的历史段）。
     * 仍生效的绑定 end_date 为 NULL，按“至今”处理（rangeEnd 封顶由调用方做）。
     * 结果按开始日、ID 升序，对应结算单里的逐段明细顺序。
     */
    @Query("SELECT b FROM Binding b WHERE b.teamId = :teamId " +
            "AND b.startDate <= :rangeEnd " +
            "AND (b.endDate IS NULL OR b.endDate >= :rangeStart) " +
            "ORDER BY b.startDate ASC, b.id ASC")
    List<Binding> findOverlappingByTeamId(@Param("teamId") Long teamId,
                                         @Param("rangeStart") LocalDate rangeStart,
                                         @Param("rangeEnd") LocalDate rangeEnd);
}