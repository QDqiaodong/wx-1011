package com.paddling.repository;

import com.paddling.entity.Rack;
import com.paddling.enums.MileageRange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RackRepository extends JpaRepository<Rack, Long> {
    Optional<Rack> findByCode(String code);
    
    Page<Rack> findByMileageRange(MileageRange mileageRange, Pageable pageable);
    
    List<Rack> findByMileageRange(MileageRange mileageRange);
    
    boolean existsByCode(String code);
    
    long countByMileageRange(MileageRange mileageRange);
}