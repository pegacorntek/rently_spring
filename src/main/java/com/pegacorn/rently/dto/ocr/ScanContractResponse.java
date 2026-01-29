package com.pegacorn.rently.dto.ocr;

public record ScanContractResponse(
    String html,
    String suggestedName
) {}
