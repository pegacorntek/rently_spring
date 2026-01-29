package com.pegacorn.rently.specification;

import com.pegacorn.rently.entity.ActivityLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class ActivityLogSpecification {

    public static Specification<ActivityLog> hasType(ActivityLog.ActivityType type) {
        return (root, query, cb) -> {
            if (type == null) return null;
            return cb.equal(root.get("type"), type);
        };
    }

    public static Specification<ActivityLog> hasTypeString(String type) {
        return (root, query, cb) -> {
            if (type == null || type.trim().isEmpty()) return null;
            try {
                ActivityLog.ActivityType activityType = ActivityLog.ActivityType.valueOf(type.toUpperCase());
                return cb.equal(root.get("type"), activityType);
            } catch (IllegalArgumentException e) {
                return null; // Invalid type, no filter
            }
        };
    }

    public static Specification<ActivityLog> hasUserId(String userId) {
        return (root, query, cb) -> {
            if (userId == null || userId.trim().isEmpty()) return null;
            return cb.equal(root.get("landlordId"), userId);
        };
    }

    public static Specification<ActivityLog> createdAfter(LocalDateTime startDate) {
        return (root, query, cb) -> {
            if (startDate == null) return null;
            return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
        };
    }

    public static Specification<ActivityLog> createdBefore(LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (endDate == null) return null;
            return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
        };
    }

    public static Specification<ActivityLog> createdBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) return null;
            if (startDate != null && endDate != null) {
                return cb.between(root.get("createdAt"), startDate, endDate);
            } else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            } else {
                return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
            }
        };
    }
}
