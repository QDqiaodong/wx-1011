package com.paddling.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "binding_history", indexes = {
    @Index(name = "idx_binding_id", columnList = "bindingId"),
    @Index(name = "idx_changed_at", columnList = "changedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BindingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "binding_id", nullable = false)
    private Long bindingId;
    
    @Column(name = "old_rack_id")
    private Long oldRackId;
    
    @Column(name = "new_rack_id", nullable = false)
    private Long newRackId;
    
    @Column(name = "change_reason", length = 500)
    private String changeReason;
    
    @Column(name = "operator", length = 100)
    private String operator = "system";
    
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;
    
    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}