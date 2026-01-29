package com.pegacorn.rently.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 100)
    private String email;

    @Column(name = "id_number", unique = true, length = 20)
    private String idNumber;

    @Column(name = "id_issue_date")
    private LocalDate idIssueDate;

    @Column(name = "id_issue_place", length = 100)
    private String idIssuePlace;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "place_of_origin")
    private String placeOfOrigin;

    @Column(name = "place_of_residence")
    private String placeOfResidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "share_full_name", nullable = false)
    private boolean shareFullName = true;

    @Column(name = "share_phone", nullable = false)
    private boolean sharePhone = false;

    @Column(name = "share_origin", nullable = false)
    private boolean shareOrigin = false;

    @Column(name = "share_gender", nullable = false)
    private boolean shareGender = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Bank account settings for VietQR payment
    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "bank_account_holder", length = 100)
    private String bankAccountHolder;

    @Transient
    private List<Role> roles;

    public enum UserStatus {
        ACTIVE, LOCKED, DELETED
    }

    public enum Role {
        LANDLORD, TENANT, SYSTEM_ADMIN
    }

    public enum Gender {
        male, female, other
    }
}
