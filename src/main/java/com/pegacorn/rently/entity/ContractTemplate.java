package com.pegacorn.rently.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contract_templates")
public class ContractTemplate {
    @Id
    private String id;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "house_id")
    private String houseId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private String houseName;
}
