package com.smart_task.smart_task_manager_backend.Controller;

import com.smart_task.smart_task_manager_backend.Model.Task;
import com.smart_task.smart_task_manager_backend.Model.User;
import com.smart_task.smart_task_manager_backend.Repository.TaskRepository;
import com.smart_task.smart_task_manager_backend.Repository.UserRepository;
import com.smart_task.smart_task_manager_backend.Security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    // ✅ Create a new task (Admin only - with user assignment)
    @PostMapping
    public ResponseEntity<?> createTask(
            @RequestBody Map<String, Object> taskData,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        System.out.println("📥 POST /api/tasks - Creating new task");
        System.out.println("Request body: " + taskData);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("❌ Unauthorized: Token missing");
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body("❌ Unauthorized: Invalid token");
            }

            if (!jwtService.isAdmin(token)) {
                return ResponseEntity.status(403).body("❌ Forbidden: Only ADMIN can create tasks");
            }

            String adminEmail = jwtService.extractEmail(token);
            System.out.println("✅ Admin " + adminEmail + " creating new task");

            Task task = new Task();
            task.setTitle((String) taskData.get("title"));
            task.setPriority((String) taskData.get("priority"));
            task.setStatus((String) taskData.get("status"));
            task.setDueDate(LocalDate.parse((String) taskData.get("dueDate")));
            task.setPercentage((Integer) taskData.getOrDefault("percentage", 0));
            task.setRemark((String) taskData.getOrDefault("remark", ""));
            task.setTaskReferenceId((String) taskData.get("taskReferenceId"));

            // ✅ NEW: Save the admin who assigned this task
            User adminUser = userRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> new RuntimeException("Admin user not found"));
            task.setAssignedBy(adminUser);
            System.out.println("✅ Task assigned BY admin: " + adminUser.getName());

            // Handle assigned user
            if (taskData.containsKey("assignedTo")) {
                Map<String, Object> assignedToData = (Map<String, Object>) taskData.get("assignedTo");
                if (assignedToData != null && assignedToData.containsKey("id")) {
                    Long userId = ((Number) assignedToData.get("id")).longValue();
                    User assignedUser = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    task.setAssignedTo(assignedUser);
                    System.out.println("✅ Task assigned TO user: " + assignedUser.getName());
                }
            }

            Task savedTask = taskRepository.save(task);
            System.out.println("✅ Task created with ID: " + savedTask.getId());

            return ResponseEntity.ok(savedTask);

        } catch (Exception e) {
            System.err.println("❌ Error creating task: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Failed to create task: " + e.getMessage());
        }
    }

    // ✅ Get all tasks (filtered by user role) - SORTED BY LATEST FIRST
    @GetMapping
    public ResponseEntity<?> getAllTasks(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        System.out.println("📥 GET /api/tasks - Fetching tasks");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("❌ Unauthorized: Token missing");
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body("❌ Unauthorized: Invalid token");
            }

            String email = jwtService.extractEmail(token);
            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Task> tasks;

            if (jwtService.isAdmin(token)) {
                tasks = taskRepository.findAll();
                System.out.println("✅ Admin " + email + " viewing all tasks: " + tasks.size());
            } else {
                tasks = taskRepository.findByAssignedTo(currentUser);
                System.out.println("✅ User " + email + " viewing assigned tasks: " + tasks.size());
            }

            // Sort latest first
            tasks = tasks.stream()
                    .sorted((t1, t2) -> Long.compare(t2.getId(), t1.getId()))
                    .collect(Collectors.toList());

            // Check and mark delayed tasks for non-admin users
            if (!jwtService.isAdmin(token)) {
                LocalDate today = LocalDate.now();
                tasks.forEach(task -> {
                    if (task.getDueDate() != null &&
                        task.getDueDate().isBefore(today) &&
                        !"Complete".equalsIgnoreCase(task.getStatus()) &&
                        !"Completed".equalsIgnoreCase(task.getStatus())) {
                        if (task.getIsDelayed() == null || !task.getIsDelayed()) {
                            task.setIsDelayed(true);
                            taskRepository.save(task);
                        }
                    }
                });
            }

            System.out.println("📊 Returning " + tasks.size() + " tasks");
            return ResponseEntity.ok(tasks);

        } catch (Exception e) {
            System.err.println("❌ Error fetching tasks: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Failed to fetch tasks: " + e.getMessage());
        }
    }

    // ✅ Get task by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        System.out.println("📥 GET /api/tasks/" + id);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("❌ Unauthorized: Token missing");
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body("❌ Unauthorized: Invalid token");
            }

            Task task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            if (!jwtService.isAdmin(token)) {
                String email = jwtService.extractEmail(token);
                User currentUser = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(currentUser.getId())) {
                    return ResponseEntity.status(403).body("❌ Forbidden: You can only view your assigned tasks");
                }
            }

            return ResponseEntity.ok(task);

        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("❌ " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Failed to fetch task: " + e.getMessage());
        }
    }

    // ✅ Update task
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        System.out.println("📝 PUT /api/tasks/" + id);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("❌ Unauthorized: Token missing");
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body("❌ Unauthorized: Invalid token");
            }

            Task task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            boolean isAdmin = jwtService.isAdmin(token);
            String email = jwtService.extractEmail(token);

            if (!isAdmin) {
                User currentUser = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(currentUser.getId())) {
                    return ResponseEntity.status(403).body("❌ Forbidden: You can only update your assigned tasks");
                }

                if (updates.containsKey("status")) task.setStatus((String) updates.get("status"));
                if (updates.containsKey("percentage")) task.setPercentage((Integer) updates.get("percentage"));
                if (updates.containsKey("expectedCompletionDate") && updates.get("expectedCompletionDate") != null) {
                    task.setExpectedCompletionDate(LocalDate.parse((String) updates.get("expectedCompletionDate")));
                }
                if (updates.containsKey("delayReason")) task.setDelayReason((String) updates.get("delayReason"));
                if (updates.containsKey("isDelayed")) task.setIsDelayed((Boolean) updates.get("isDelayed"));

            } else {
                if (updates.containsKey("title")) task.setTitle((String) updates.get("title"));
                if (updates.containsKey("priority")) task.setPriority((String) updates.get("priority"));
                if (updates.containsKey("status")) task.setStatus((String) updates.get("status"));
                if (updates.containsKey("dueDate")) task.setDueDate(LocalDate.parse((String) updates.get("dueDate")));
                if (updates.containsKey("percentage")) task.setPercentage((Integer) updates.get("percentage"));
                if (updates.containsKey("remark")) task.setRemark((String) updates.get("remark"));
                if (updates.containsKey("taskReferenceId")) task.setTaskReferenceId((String) updates.get("taskReferenceId"));
                if (updates.containsKey("expectedCompletionDate") && updates.get("expectedCompletionDate") != null) {
                    task.setExpectedCompletionDate(LocalDate.parse((String) updates.get("expectedCompletionDate")));
                }
                if (updates.containsKey("delayReason")) task.setDelayReason((String) updates.get("delayReason"));
                if (updates.containsKey("isDelayed")) task.setIsDelayed((Boolean) updates.get("isDelayed"));
                if (updates.containsKey("assignedTo")) {
                    Map<String, Object> assignedToData = (Map<String, Object>) updates.get("assignedTo");
                    if (assignedToData != null && assignedToData.containsKey("id")) {
                        Long userId = ((Number) assignedToData.get("id")).longValue();
                        User assignedUser = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                        task.setAssignedTo(assignedUser);
                    }
                }
            }

            Task updatedTask = taskRepository.save(task);
            return ResponseEntity.ok(updatedTask);

        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("❌ " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Failed to update task: " + e.getMessage());
        }
    }

    // ✅ Delete task (Admin only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        System.out.println("🗑️ DELETE /api/tasks/" + id);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("❌ Unauthorized: Token missing");
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body("❌ Unauthorized: Invalid token");
            }
            if (!jwtService.isAdmin(token)) {
                return ResponseEntity.status(403).body("❌ Forbidden: Only ADMIN can delete tasks");
            }

            String adminEmail = jwtService.extractEmail(token);
            Task task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            taskRepository.delete(task);
            System.out.println("✅ Task #" + id + " deleted by " + adminEmail);
            return ResponseEntity.ok("✅ Task deleted successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("❌ " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Failed to delete task: " + e.getMessage());
        }
    }
}
