package com.paddling.entity;

import com.paddling.enums.MileageRange;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rack", indexes = {
    @Index(name = "idx_mileage_range", columnList = "mileageRange")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;
    
    @Column(name = "capacity", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "mileage_range", nullable = false, length = 20)
    private MileageRange mileageRange;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}