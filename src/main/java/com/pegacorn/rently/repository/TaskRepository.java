package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    @Query("SELECT t FROM Task t WHERE t.userId = :userId ORDER BY t.isPinned DESC, t.isDone ASC, t.createdAt DESC")
    List<Task> findByUserIdOrderByPinnedAndDone(@Param("userId") String userId);

    List<Task> findByUserIdAndIsDoneFalse(String userId);

    boolean existsByUserIdAndTitle(String userId, String title);
}
