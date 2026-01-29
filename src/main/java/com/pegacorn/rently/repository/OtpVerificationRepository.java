package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, String> {

    // Find unverified OTP for verification step
    @Query("SELECT o FROM OtpVerification o WHERE o.phone = :phone AND o.type = :type AND o.verified = false AND o.expiresAt > :now ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpVerification> findLatestUnverifiedOtp(
            @Param("phone") String phone,
            @Param("type") OtpVerification.OtpType type,
            @Param("now") LocalDateTime now);

    // Find verified OTP for registration/reset step
    @Query("SELECT o FROM OtpVerification o WHERE o.phone = :phone AND o.type = :type AND o.verified = true AND o.expiresAt > :now ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpVerification> findLatestVerifiedOtp(
            @Param("phone") String phone,
            @Param("type") OtpVerification.OtpType type,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :now OR o.verified = true")
    void deleteExpiredOrUsedOtps(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.phone = :phone")
    void deleteByPhone(@Param("phone") String phone);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.phone = :phone AND o.type = :type")
    void deleteByPhoneAndType(@Param("phone") String phone, @Param("type") OtpVerification.OtpType type);

    @Modifying
    @Query("UPDATE OtpVerification o SET o.verified = true WHERE o.id = :id")
    void markAsVerified(@Param("id") String id);

    // Find latest OTP for rate limiting (regardless of verification status)
    @Query("SELECT o FROM OtpVerification o WHERE o.phone = :phone AND o.type = :type ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpVerification> findLatestByPhoneAndType(
            @Param("phone") String phone,
            @Param("type") OtpVerification.OtpType type);
}
