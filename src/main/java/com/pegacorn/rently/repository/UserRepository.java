package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByIdNumber(String idNumber);

    boolean existsByIdNumberAndIdNot(String idNumber, String id);

    Optional<User> findByIdNumber(String idNumber);

    @Query("SELECT u FROM User u JOIN RoomTenant rt ON rt.userId = u.id WHERE rt.roomId = :roomId AND rt.leftAt IS NULL")
    List<User> findTenantsByRoomId(@Param("roomId") String roomId);
}
