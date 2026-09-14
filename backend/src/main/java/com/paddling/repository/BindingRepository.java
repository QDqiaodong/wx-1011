package com.paddling.repository;

import com.paddling.entity.Binding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}