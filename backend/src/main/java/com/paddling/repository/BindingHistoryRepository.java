package com.paddling.repository;

import com.paddling.entity.BindingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BindingHistoryRepository extends JpaRepository<BindingHistory, Long> {
    Page<BindingHistory> findByBindingId(Long bindingId, Pageable pageable);
    
    List<BindingHistory> findByBindingIdOrderByChangedAtDesc(Long bindingId);
}