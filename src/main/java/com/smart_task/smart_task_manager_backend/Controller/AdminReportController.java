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
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    // ✅ Get all users with their task statistics (Admin only)
    @GetMapping("/users")
    public ResponseEntity<?> getUserReports(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        System.out.println("📊 GET /api/admin/reports/users - Fetching user reports");

        // Check authentication
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.err.println("❌ Unauthorized: Token missing");
            return ResponseEntity.status(401).body("❌ Unauthorized: Token missing");
        }

        String token = authHeader.substring(7);

        try {
            // Validate token
            if (!jwtService.validateToken(token)) {
                System.err.println("❌ Unauthorized: Invalid token");
                return ResponseEntity.status(401).body("❌ Unauthorized: Invalid token");
            }

            // Check if user is ADMIN
            if (!jwtService.isAdmin(token)) {
                System.err.println("❌ Forbidden: Only ADMIN can access reports");
                return ResponseEntity.status(403).body("❌ Forbidden: Only ADMIN can access reports");
            }

            System.out.println("✅ Admin access verified, generating reports...");

            List<User> users = userRepository.findAll();
            List<Map<String, Object>> reports = new ArrayList<>();

            for (User user : users) {
                List<Task> userTasks = taskRepository.findByAssignedTo(user);

                // Calculate statistics
                long totalTasks = userTasks.size();
                long completedTasks = userTasks.stream()
                        .filter(task -> "Complete".equalsIgnoreCase(task.getStatus()) || 
                                      "Completed".equalsIgnoreCase(task.getStatus()))
                        .count();
                long inProgressTasks = userTasks.stream()
                        .filter(task -> "Incomplete".equalsIgnoreCase(task.getStatus()) || 
                                      "In Progress".equalsIgnoreCase(task.getStatus()))
                        .count();
                long pendingTasks = userTasks.stream()
                        .filter(task -> "Pending".equalsIgnoreCase(task.getStatus()) || 
                                      "To-Do".equalsIgnoreCase(task.getStatus()))
                        .count();

                // ✅ Calculate delayed tasks
                LocalDate today = LocalDate.now();
                long delayedTasks = userTasks.stream()
                        .filter(task -> {
                            boolean isNotComplete = !"Complete".equalsIgnoreCase(task.getStatus()) && 
                                                   !"Completed".equalsIgnoreCase(task.getStatus());
                            boolean isPastDue = task.getDueDate() != null && 
                                              task.getDueDate().isBefore(today);
                            return isNotComplete && isPastDue;
                        })
                        .count();

                Map<String, Object> report = new HashMap<>();
                report.put("id", user.getId());
                report.put("name", user.getName());
                report.put("email", user.getEmail());
                report.put("role", user.getRole().name());
                report.put("active", user.isActive());
                report.put("totalTasks", totalTasks);
                report.put("completedTasks", completedTasks);
                report.put("inProgressTasks", inProgressTasks);
                report.put("pendingTasks", pendingTasks);
                report.put("delayedTasks", delayedTasks);

                reports.add(report);

                System.out.println("📋 User: " + user.getName() + " - Total: " + totalTasks + 
                                 ", Completed: " + completedTasks + ", Delayed: " + delayedTasks);
            }

            System.out.println("✅ Successfully generated reports for " + users.size() + " users");
            return ResponseEntity.ok(reports);

        } catch (Exception e) {
            System.err.println("❌ Error generating reports: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Failed to generate reports: " + e.getMessage());
        }
    }

    // ✅ Get all tasks for a specific user (Admin only) - SORTED BY LATEST FIRST
    @GetMapping("/users/{userId}/tasks")
    public ResponseEntity<?> getUserTasks(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        System.out.println("📥 GET /api/admin/reports/users/" + userId + "/tasks - Fetching user tasks");

        // Check authentication
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.err.println("❌ Unauthorized: Token missing");
            return ResponseEntity.status(401).body("❌ Unauthorized: Token missing");
        }

        String token = authHeader.substring(7);

        try {
            // Validate token
            if (!jwtService.validateToken(token)) {
                System.err.println("❌ Unauthorized: Invalid token");
                return ResponseEntity.status(401).body("❌ Unauthorized: Invalid token");
            }

            // Check if user is ADMIN
            if (!jwtService.isAdmin(token)) {
                System.err.println("❌ Forbidden: Only ADMIN can access user tasks");
                return ResponseEntity.status(403).body("❌ Forbidden: Only ADMIN can access user tasks");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("✅ Fetching tasks for user: " + user.getName());

            List<Task> tasks = taskRepository.findByAssignedTo(user);

            // ✅ SORT TASKS BY LATEST FIRST (by ID descending)
            tasks = tasks.stream()
                    .sorted((t1, t2) -> Long.compare(t2.getId(), t1.getId()))
                    .collect(Collectors.toList());

            // ✅ Calculate and mark delayed tasks
            LocalDate today = LocalDate.now();
            tasks.forEach(task -> {
                if (task.getDueDate() != null && 
                    task.getDueDate().isBefore(today) && 
                    !"Complete".equalsIgnoreCase(task.getStatus()) && 
                    !"Completed".equalsIgnoreCase(task.getStatus())) {
                    
                    // Mark as delayed if not already marked
                    if (task.getIsDelayed() == null || !task.getIsDelayed()) {
                        task.setIsDelayed(true);
                        taskRepository.save(task);
                        System.out.println("⚠️ Task #" + task.getId() + " marked as delayed");
                    }
                }
            });

            System.out.println("✅ Found " + tasks.size() + " tasks for user " + user.getName() + " (sorted by latest)");
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            System.err.println("❌ User not found: " + userId);
            return ResponseEntity.status(404).body("❌ " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error fetching user tasks: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Failed to fetch user tasks: " + e.getMessage());
        }
    }

    // ✅ Update task from admin report (Admin only - can edit ALL fields)
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<?> updateTaskFromReport(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> updates,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        System.out.println("📝 PUT /api/admin/reports/tasks/" + taskId + " - Admin updating task");
        System.out.println("Updates: " + updates);

        // Check authentication
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.err.println("❌ Unauthorized: Token missing");
            return ResponseEntity.status(401).body("❌ Unauthorized: Token missing");
        }

        String token = authHeader.substring(7);

        try {
            // Validate token
            if (!jwtService.validateToken(token)) {
                System.err.println("❌ Unauthorized: Invalid token");
                return ResponseEntity.status(401).body("❌ Unauthorized: Invalid token");
            }

            // Check if user is ADMIN
            if (!jwtService.isAdmin(token)) {
                System.err.println("❌ Forbidden: Only ADMIN can update tasks from reports");
                return ResponseEntity.status(403).body("❌ Forbidden: Only ADMIN can update tasks");
            }

            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            System.out.println("📋 Current task state - Status: " + task.getStatus() + 
                             ", Progress: " + task.getPercentage() + "%");

            // ✅ Admin can update ALL fields
            if (updates.containsKey("title")) {
                task.setTitle((String) updates.get("title"));
                System.out.println("✏️ Updated title: " + updates.get("title"));
            }
            if (updates.containsKey("priority")) {
                task.setPriority((String) updates.get("priority"));
                System.out.println("✏️ Updated priority: " + updates.get("priority"));
            }
            if (updates.containsKey("status")) {
                String newStatus = (String) updates.get("status");
                System.out.println("✏️ Updated status: " + task.getStatus() + " → " + newStatus);
                task.setStatus(newStatus);
            }
            if (updates.containsKey("dueDate")) {
                LocalDate newDueDate = LocalDate.parse((String) updates.get("dueDate"));
                System.out.println("✏️ Updated due date: " + newDueDate);
                task.setDueDate(newDueDate);
            }
            if (updates.containsKey("percentage")) {
                Integer newPercentage = (Integer) updates.get("percentage");
                System.out.println("✏️ Updated progress: " + task.getPercentage() + "% → " + newPercentage + "%");
                task.setPercentage(newPercentage);
            }
            if (updates.containsKey("remark")) {
                task.setRemark((String) updates.get("remark"));
                System.out.println("✏️ Updated remark");
            }
            if (updates.containsKey("taskReferenceId")) {
                task.setTaskReferenceId((String) updates.get("taskReferenceId"));
                System.out.println("✏️ Updated task reference ID: " + updates.get("taskReferenceId"));
            }

            // ✅ Update delay information
            if (updates.containsKey("expectedCompletionDate") && updates.get("expectedCompletionDate") != null) {
                LocalDate expectedDate = LocalDate.parse((String) updates.get("expectedCompletionDate"));
                task.setExpectedCompletionDate(expectedDate);
                System.out.println("✏️ Updated expected completion date: " + expectedDate);
            }
            if (updates.containsKey("delayReason")) {
                task.setDelayReason((String) updates.get("delayReason"));
                System.out.println("✏️ Updated delay reason: " + updates.get("delayReason"));
            }
            if (updates.containsKey("isDelayed")) {
                task.setIsDelayed((Boolean) updates.get("isDelayed"));
                System.out.println("✏️ Updated delay status: " + updates.get("isDelayed"));
            }

            // ✅ Update assigned user
            if (updates.containsKey("assignedTo")) {
                Map<String, Object> assignedToData = (Map<String, Object>) updates.get("assignedTo");
                if (assignedToData != null && assignedToData.containsKey("id")) {
                    Long userId = ((Number) assignedToData.get("id")).longValue();
                    User assignedUser = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    task.setAssignedTo(assignedUser);
                    System.out.println("✏️ Updated assigned user: " + assignedUser.getName());
                }
            }

            Task updatedTask = taskRepository.save(task);
            System.out.println("✅ Task #" + taskId + " updated successfully by admin");

            return ResponseEntity.ok(updatedTask);

        } catch (RuntimeException e) {
            System.err.println("❌ Task not found: " + taskId);
            return ResponseEntity.status(404).body("❌ " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error updating task: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Failed to update task: " + e.getMessage());
        }
    }
}