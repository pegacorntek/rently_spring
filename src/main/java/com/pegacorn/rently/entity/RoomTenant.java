package com.pegacorn.rently.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@Entity
@Table(name = "room_tenants")
public class RoomTenant {
    @Id
    private String id;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Transient
    private String fullName;

    @Transient
    private String phone;

    @Transient
    private String idNumber;

    @Transient
    private User.Gender gender;

    @Transient
    private LocalDate dateOfBirth;

    @Transient
    private String placeOfOrigin;

    @Transient
    private LocalDate idIssueDate;

    @Transient
    private String idIssuePlace;

    @Transient
    private boolean shareFullName;

    @Transient
    private boolean sharePhone;

    @Transient
    private boolean shareOrigin;

    @Transient
    private boolean shareGender;

    // Constructor for JPA entity fields only
    public RoomTenant(String id, String roomId, String userId, boolean isPrimary,
            LocalDateTime joinedAt, LocalDateTime leftAt) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.isPrimary = isPrimary;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
    }

    // Constructor including transient user fields (for JPQL projection)
    public RoomTenant(String id, String roomId, String userId, boolean isPrimary,
            LocalDateTime joinedAt, LocalDateTime leftAt,
            String fullName, String phone,
            String idNumber, User.Gender gender, LocalDate dateOfBirth, String placeOfOrigin,
            LocalDate idIssueDate, String idIssuePlace,
            boolean shareFullName, boolean sharePhone, boolean shareOrigin, boolean shareGender) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.isPrimary = isPrimary;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.fullName = fullName;
        this.phone = phone;
        this.idNumber = idNumber;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.placeOfOrigin = placeOfOrigin;
        this.idIssueDate = idIssueDate;
        this.idIssuePlace = idIssuePlace;
        this.shareFullName = shareFullName;
        this.sharePhone = sharePhone;
        this.shareOrigin = shareOrigin;
        this.shareGender = shareGender;
    }
}
