package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, String> {

    List<PushSubscription> findByUserIdAndIsActiveTrue(String userId);

    List<PushSubscription> findByUserId(String userId);

    Optional<PushSubscription> findByUserIdAndFcmToken(String userId, String fcmToken);

    Optional<PushSubscription> findByFcmToken(String fcmToken);

    boolean existsByFcmToken(String fcmToken);

    @Query("SELECT ps FROM PushSubscription ps WHERE ps.isActive = true")
    List<PushSubscription> findAllActive();

    @Modifying
    @Query("UPDATE PushSubscription ps SET ps.isActive = false WHERE ps.fcmToken = :fcmToken")
    int deactivateByToken(@Param("fcmToken") String fcmToken);

    @Modifying
    @Query("DELETE FROM PushSubscription ps WHERE ps.fcmToken = :fcmToken")
    int deleteByFcmToken(@Param("fcmToken") String fcmToken);
}
