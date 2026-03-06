package com.smart_task.smart_task_manager_backend.Repository;

import com.smart_task.smart_task_manager_backend.Model.Task;
import com.smart_task.smart_task_manager_backend.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // ✅ Find all tasks assigned to a specific user
    List<Task> findByAssignedTo(User user);
}
