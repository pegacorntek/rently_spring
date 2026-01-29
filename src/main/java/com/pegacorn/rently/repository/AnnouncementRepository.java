package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, String> {
    Page<Announcement> findByStatus(Announcement.AnnouncementStatus status, Pageable pageable);

    @Query("SELECT a FROM Announcement a WHERE a.status = 'PUBLISHED' " +
           "AND (a.publishAt IS NULL OR a.publishAt <= :now) " +
           "AND (a.expireAt IS NULL OR a.expireAt > :now) " +
           "AND (a.targetAudience = 'ALL' OR a.targetAudience = :audience) " +
           "ORDER BY a.createdAt DESC")
    List<Announcement> findActiveAnnouncements(@Param("now") LocalDateTime now, @Param("audience") Announcement.TargetAudience audience);

    Page<Announcement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
