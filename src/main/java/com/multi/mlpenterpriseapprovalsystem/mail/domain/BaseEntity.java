package com.multi.mlpenterpriseapprovalsystem.mail.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : BaseEntity
 * @since : 2025-12-30 화요일
 */

@MappedSuperclass
@Getter
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}