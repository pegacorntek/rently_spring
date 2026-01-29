package com.pegacorn.rently.specification;

import com.pegacorn.rently.entity.User;
import com.pegacorn.rently.entity.UserRole;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> hasStatus(User.UserStatus status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<User> notDeleted() {
        return (root, query, cb) -> cb.notEqual(root.get("status"), User.UserStatus.DELETED);
    }

    public static Specification<User> isDeleted() {
        return (root, query, cb) -> cb.equal(root.get("status"), User.UserStatus.DELETED);
    }

    public static Specification<User> searchByNameOrPhone(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) return null;
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("fullName")), pattern),
                cb.like(root.get("phone"), "%" + search + "%")
            );
        };
    }

    public static Specification<User> hasRole(User.Role role, String userRoleTableAlias) {
        return (root, query, cb) -> {
            if (role == null) return null;
            // Subquery to check if user has the specified role
            Subquery<String> subquery = query.subquery(String.class);
            Root<UserRole> userRoleRoot = subquery.from(UserRole.class);
            subquery.select(userRoleRoot.get("userId"))
                    .where(cb.and(
                        cb.equal(userRoleRoot.get("userId"), root.get("id")),
                        cb.equal(userRoleRoot.get("role"), role)
                    ));
            return cb.exists(subquery);
        };
    }

    public static Specification<User> includeDeleted(boolean includeDeleted) {
        return (root, query, cb) -> {
            if (includeDeleted) {
                return null; // No filter, include all
            }
            return cb.notEqual(root.get("status"), User.UserStatus.DELETED);
        };
    }
}
