package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.User;
import com.pegacorn.rently.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, String> {

    List<UserRole> findByUserId(String userId);

    @Query("SELECT ur.role FROM UserRole ur WHERE ur.userId = :userId")
    List<User.Role> findRolesByUserId(@Param("userId") String userId);

    boolean existsByUserIdAndRole(String userId, User.Role role);

    void deleteByUserId(String userId);
}
