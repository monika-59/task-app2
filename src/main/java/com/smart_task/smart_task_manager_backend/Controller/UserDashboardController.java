// UserDashboardController.java
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
@RequestMapping("/api/user/dashboard")
public class UserDashboardController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    // ✅ Get user dashboard statistics
    @GetMapping("/stats")
    public ResponseEntity<?> getUserDashboardStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        System.out.println("📊 GET /api/user/dashboard/stats - Fetching user statistics");

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

            System.out.println("👤 Calculating stats for user: " + currentUser.getName() + " (ID: " + currentUser.getId() + ")");

            // Get all tasks assigned to this user
            List<Task> userTasks = taskRepository.findByAssignedTo(currentUser);
            
            // Calculate statistics
            int totalTasks = userTasks.size();
            int completedTasks = 0;
            int incompleteTasks = 0;
            int onHoldTasks = 0;
            int delayedTasks = 0;
            int dueTodayTasks = 0;
            int upcomingTasks = 0;
            int highPriorityTasks = 0;
            int mediumPriorityTasks = 0;
            int lowPriorityTasks = 0;
            int totalProgress = 0;

            LocalDate today = LocalDate.now();

            for (Task task : userTasks) {
                // Status-based counting
                String status = task.getStatus() != null ? task.getStatus().toLowerCase() : "";
                if (status.equals("complete") || status.equals("completed")) {
                    completedTasks++;
                } else if (status.equals("incomplete")) {
                    incompleteTasks++;
                } else if (status.contains("hold")) {
                    onHoldTasks++;
                }

                // Priority-based counting
                String priority = task.getPriority() != null ? task.getPriority().toLowerCase() : "";
                if (priority.equals("high")) {
                    highPriorityTasks++;
                } else if (priority.equals("medium")) {
                    mediumPriorityTasks++;
                } else if (priority.equals("low")) {
                    lowPriorityTasks++;
                }

                // Delayed tasks
                if (task.getDueDate() != null && task.getDueDate().isBefore(today) && 
                    !status.equals("complete") && !status.equals("completed")) {
                    delayedTasks++;
                }

                // Due today
                if (task.getDueDate() != null && task.getDueDate().isEqual(today)) {
                    dueTodayTasks++;
                }

                // Upcoming (next 7 days)
                if (task.getDueDate() != null && 
                    task.getDueDate().isAfter(today) && 
                    task.getDueDate().isBefore(today.plusDays(8))) {
                    upcomingTasks++;
                }

                // Total progress
                totalProgress += (task.getPercentage() != null ? task.getPercentage() : 0);
            }

            // Calculate average progress
            int averageProgress = totalTasks > 0 ? totalProgress / totalTasks : 0;

            // Calculate completion rate
            double completionRate = totalTasks > 0 ? ((double) completedTasks / totalTasks) * 100 : 0;

            // Build response
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTasks", totalTasks);
            stats.put("completedTasks", completedTasks);
            stats.put("incompleteTasks", incompleteTasks);
            stats.put("onHoldTasks", onHoldTasks);
            stats.put("delayedTasks", delayedTasks);
            stats.put("dueTodayTasks", dueTodayTasks);
            stats.put("upcomingTasks", upcomingTasks);
            stats.put("highPriorityTasks", highPriorityTasks);
            stats.put("mediumPriorityTasks", mediumPriorityTasks);
            stats.put("lowPriorityTasks", lowPriorityTasks);
            stats.put("averageProgress", averageProgress);
            stats.put("completionRate", Math.round(completionRate * 10) / 10.0); // Round to 1 decimal

            // User info
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", currentUser.getId());
            userInfo.put("name", currentUser.getName());
            userInfo.put("email", currentUser.getEmail());
            userInfo.put("role", currentUser.getRole().name());

            Map<String, Object> response = new HashMap<>();
            response.put("user", userInfo);
            response.put("statistics", stats);
            response.put("lastUpdated", LocalDate.now().toString());

            System.out.println("✅ Stats calculated - Total: " + totalTasks + ", Completed: " + completedTasks + ", Delayed: " + delayedTasks);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error fetching dashboard stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Failed to fetch dashboard statistics");
        }
    }

    // ✅ Get recent tasks for user dashboard
    @GetMapping("/recent-tasks")
    public ResponseEntity<?> getRecentTasks(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "5") int limit
    ) {
        System.out.println("📋 GET /api/user/dashboard/recent-tasks - Fetching recent tasks (limit: " + limit + ")");

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

            System.out.println("👤 Fetching recent tasks for user: " + currentUser.getName());

            // Get all tasks assigned to this user
            List<Task> allUserTasks = taskRepository.findByAssignedTo(currentUser);

            // Sort tasks by priority (High first) and due date (closest first)
            List<Task> recentTasks = allUserTasks.stream()
                    .filter(task -> {
                        String status = task.getStatus() != null ? task.getStatus().toLowerCase() : "";
                        return !status.equals("complete") && !status.equals("completed");
                    })
                    .sorted((t1, t2) -> {
                        // First sort by priority
                        int priorityCompare = getPriorityValue(t2.getPriority()) - getPriorityValue(t1.getPriority());
                        if (priorityCompare != 0) return priorityCompare;
                        
                        // Then by due date (null dates go last)
                        if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
                        if (t1.getDueDate() == null) return 1;
                        if (t2.getDueDate() == null) return -1;
                        return t1.getDueDate().compareTo(t2.getDueDate());
                    })
                    .limit(limit)
                    .collect(Collectors.toList());

            // Build task response
            List<Map<String, Object>> taskList = new ArrayList<>();
            for (Task task : recentTasks) {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("id", task.getId());
                taskMap.put("title", task.getTitle());
                taskMap.put("description", task.getDescription());
                taskMap.put("status", task.getStatus());
                taskMap.put("priority", task.getPriority());
                taskMap.put("dueDate", task.getDueDate() != null ? task.getDueDate().toString() : null);
                taskMap.put("percentage", task.getPercentage());
                
                if (task.getAssignedTo() != null) {
                    Map<String, Object> assignedToMap = new HashMap<>();
                    assignedToMap.put("id", task.getAssignedTo().getId());
                    assignedToMap.put("name", task.getAssignedTo().getName());
                    taskMap.put("assignedTo", assignedToMap);
                }
                
                if (task.getCreatedBy() != null) {
                    Map<String, Object> createdByMap = new HashMap<>();
                    createdByMap.put("id", ((Task) task.getCreatedBy()).getId());
                    createdByMap.put("name", ((User) task.getCreatedBy()).getName());
                    taskMap.put("createdBy", createdByMap);
                }
                
                taskList.add(taskMap);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("tasks", taskList);
            response.put("count", taskList.size());

            System.out.println("✅ Fetched " + taskList.size() + " recent tasks");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error fetching recent tasks: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Failed to fetch recent tasks");
        }
    }

    // Helper method to get priority value for sorting
    private int getPriorityValue(String priority) {
        if (priority == null) return 0;
        switch (priority.toLowerCase()) {
            case "high": return 3;
            case "medium": return 2;
            case "low": return 1;
            default: return 0;
        }
    }
}