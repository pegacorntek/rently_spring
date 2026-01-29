package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.task.CreateTaskRequest;
import com.pegacorn.rently.dto.task.TaskDto;
import com.pegacorn.rently.entity.Task;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public List<TaskDto> getAll(String userId) {
        return taskRepository.findByUserIdOrderByPinnedAndDone(userId).stream()
                .map(TaskDto::fromEntity)
                .toList();
    }

    @Transactional
    public TaskDto create(CreateTaskRequest request, String userId) {
        Task task = Task.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .title(request.title())
                .isDone(false)
                .isPinned(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        taskRepository.save(task);
        return TaskDto.fromEntity(task);
    }

    @Transactional
    public void createDefaultTask(String title, String userId) {
        // Don't create duplicate tasks with the same title
        if (taskRepository.existsByUserIdAndTitle(userId, title)) {
            return;
        }

        Task task = Task.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .title(title)
                .isDone(false)
                .isPinned(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        taskRepository.save(task);
    }

    @Transactional
    public TaskDto toggleDone(String id, String userId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.TASK_NOT_FOUND));

        if (!task.getUserId().equals(userId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        task.setDone(!task.isDone());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        return TaskDto.fromEntity(task);
    }

    @Transactional
    public TaskDto togglePin(String id, String userId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.TASK_NOT_FOUND));

        if (!task.getUserId().equals(userId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        task.setPinned(!task.isPinned());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        return TaskDto.fromEntity(task);
    }

    @Transactional
    public void delete(String id, String userId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.TASK_NOT_FOUND));

        if (!task.getUserId().equals(userId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        taskRepository.delete(task);
    }
}
