package com.smart_task.smart_task_manager_backend.Service;

import com.smart_task.smart_task_manager_backend.Model.Task;
import com.smart_task.smart_task_manager_backend.Model.User;
import com.smart_task.smart_task_manager_backend.Repository.TaskRepository;
import com.smart_task.smart_task_manager_backend.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    // ✅ Create task — captures exact assignment timestamp
    public Task createTask(Task task) {
        // ✅ FIXED: Always set exact datetime when task is created/assigned
        if (task.getCreatedDate() == null) {
            task.setCreatedDate(LocalDateTime.now());
        }

        if (task.getAssignedTo() != null && task.getAssignedTo().getId() != null) {
            User user = userRepository.findById(task.getAssignedTo().getId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + task.getAssignedTo().getId()));
            task.setAssignedTo(user);
        }

        return taskRepository.save(task);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + id));
    }

    public Task updateTask(Long id, Task updatedTask) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + id));

        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setPriority(updatedTask.getPriority());
        existingTask.setStatus(updatedTask.getStatus());
        existingTask.setDueDate(updatedTask.getDueDate());
        existingTask.setRemark(updatedTask.getRemark());
        existingTask.setPercentage(updatedTask.getPercentage());
        existingTask.setTaskReferenceId(updatedTask.getTaskReferenceId());
        // ✅ NOTE: Do NOT update createdDate on edit — preserve original assignment time

        if (updatedTask.getAssignedTo() != null && updatedTask.getAssignedTo().getId() != null) {
            User user = userRepository.findById(updatedTask.getAssignedTo().getId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + updatedTask.getAssignedTo().getId()));
            existingTask.setAssignedTo(user);
        } else {
            existingTask.setAssignedTo(null);
        }

        return taskRepository.save(existingTask);
    }

    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new RuntimeException("Task not found with ID: " + id);
        }
        taskRepository.deleteById(id);
    }
}
