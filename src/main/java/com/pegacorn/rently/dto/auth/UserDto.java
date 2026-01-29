package com.pegacorn.rently.dto.auth;

import com.pegacorn.rently.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UserDto(
        String id,
        String phone,
        String fullName,
        String email,
        String idNumber,
        LocalDate idIssueDate,
        String idIssuePlace,
        String gender,
        LocalDate dateOfBirth,
        String placeOfOrigin,
        String placeOfResidence,
        List<String> roles,
        String status,
        // Bank account settings
        String bankName,
        String bankCode,
        String bankAccountNumber,
        String bankAccountHolder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getPhone(),
                user.getFullName(),
                user.getEmail(),
                user.getIdNumber(),
                user.getIdIssueDate(),
                user.getIdIssuePlace(),
                user.getGender() != null ? user.getGender().name() : null,
                user.getDateOfBirth(),
                user.getPlaceOfOrigin(),
                user.getPlaceOfResidence(),
                user.getRoles() != null
                        ? user.getRoles().stream().map(Enum::name).toList()
                        : List.of(),
                user.getStatus().name(),
                user.getBankName(),
                user.getBankCode(),
                user.getBankAccountNumber(),
                user.getBankAccountHolder(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getDeletedAt()
        );
    }
}
