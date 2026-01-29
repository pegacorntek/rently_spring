package com.pegacorn.rently.dto.ocr;

import java.math.BigDecimal;

/**
 * Response containing both extracted structured data and full HTML content
 * from a scanned paper contract.
 */
public record ExtractContractDataResponse(
        // Full HTML content of the contract
        String htmlContent,

        // Suggested template name
        String suggestedName,

        // Extracted structured data
        ExtractedContractData extractedData) {
    public record ExtractedContractData(
            // Landlord info
            String landlordName,
            String landlordPhone,
            String landlordIdNumber,
            String landlordAddress,

            // Tenant info
            String tenantName,
            String tenantPhone,
            String tenantIdNumber,
            String tenantDateOfBirth,
            String tenantPlaceOfOrigin,
            String tenantPlaceOfResidence,
            String tenantIdIssueDate,
            String tenantIdIssuePlace,
            String tenantGender,

            // Property info
            String propertyName,
            String propertyAddress,
            String roomCode,
            Integer floor,
            BigDecimal areaM2,

            // Financial info
            BigDecimal monthlyRent,
            BigDecimal deposit,
            Integer depositMonths,
            Integer paymentDueDay,

            // Contract terms
            String startDate,
            String endDate,
            Integer durationMonths,
            String paymentCycle,

            // Utility rates
            BigDecimal electricityRate,
            BigDecimal waterRate) {
    }
}
