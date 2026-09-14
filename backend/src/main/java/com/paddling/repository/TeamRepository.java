package com.paddling.repository;

import com.paddling.entity.Team;
import com.paddling.enums.MileageRange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Page<Team> findByTrainingMileage(MileageRange trainingMileage, Pageable pageable);
    
    List<Team> findByTrainingMileage(MileageRange trainingMileage);
    
    boolean existsByName(String name);
}