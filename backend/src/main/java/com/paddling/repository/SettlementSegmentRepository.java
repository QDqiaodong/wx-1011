package com.paddling.repository;

import com.paddling.entity.SettlementSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementSegmentRepository extends JpaRepository<SettlementSegment, Long> {

    List<SettlementSegment> findBySettlement_IdOrderBySegmentNoAsc(Long settlementId);
}
