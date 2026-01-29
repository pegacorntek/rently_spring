package com.pegacorn.rently.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @Size(max = 12, message = "ID number must not exceed 12 characters")
        String idNumber,

        LocalDate idIssueDate,

        @Size(max = 100, message = "ID issue place must not exceed 100 characters")
        String idIssuePlace,

        String gender,

        LocalDate dateOfBirth,

        String placeOfOrigin,

        String placeOfResidence
) {}
