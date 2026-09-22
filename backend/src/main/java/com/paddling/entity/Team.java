package com.paddling.entity;

import com.paddling.enums.MileageRange;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "team", indexes = {
    @Index(name = "idx_training_mileage", columnList = "trainingMileage")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Team implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "member_count")
    private Integer memberCount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "training_mileage", nullable = false, length = 20)
    private MileageRange trainingMileage;
    
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
