package com.pegacorn.rently.service;

import com.pegacorn.rently.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpCleanupService {

    private final OtpVerificationRepository otpVerificationRepository;

    /**
     * Runs every 24 hours to clean up expired or used OTPs
     */
    @Scheduled(fixedRate = 24 * 60 * 60 * 1000) // 24 hours in milliseconds
    @Transactional
    public void cleanupExpiredAndUsedOtps() {
        log.info("Running OTP cleanup task...");
        otpVerificationRepository.deleteExpiredOrUsedOtps(LocalDateTime.now());
        log.info("OTP cleanup completed");
    }
}
