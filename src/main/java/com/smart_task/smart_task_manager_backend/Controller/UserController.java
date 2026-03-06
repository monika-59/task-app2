package com.smart_task.smart_task_manager_backend.Controller;

import com.smart_task.smart_task_manager_backend.Model.User;
import com.smart_task.smart_task_manager_backend.Repository.UserRepository;
import com.smart_task.smart_task_manager_backend.Security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    // ✅ Get all users (Admin only) - for task assignment dropdown
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        System.out.println("📥 GET /api/users - Fetching all users");

        // Check authentication
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("❌ Unauthorized: Token missing");
        }

        String token = authHeader.substring(7);

        try {
            // Validate token
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body("❌ Unauthorized: Invalid token");
            }

            // Check if user is ADMIN
            if (!jwtService.isAdmin(token)) {
                return ResponseEntity.status(403).body("❌ Forbidden: Only ADMIN can view all users");
            }

            List<User> users = userRepository.findAll();

            // ✅ FIXED: Return only necessary fields (exclude password and sensitive data)
            List<Map<String, Object>> userList = users.stream()
                    .filter(User::isActive) // Only active users
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("name", user.getName());
                        userMap.put("email", user.getEmail());
                        userMap.put("role", user.getRole().name());
                        return userMap;
                    })
                    .collect(Collectors.toList());

            System.out.println("✅ Found " + userList.size() + " active users");
            return ResponseEntity.ok(userList);

        } catch (Exception e) {
            System.err.println("❌ Error fetching users: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Failed to fetch users: " + e.getMessage());
        }
    }

    // ✅ Get user by ID (for verification)
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        System.out.println("📥 GET /api/users/" + id);

        // Check authentication
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("❌ Unauthorized: Token missing");
        }

        String token = authHeader.substring(7);

        try {
            // Validate token
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(401).body("❌ Unauthorized: Invalid token");
            }

            // Check if user is ADMIN or requesting their own data
            String email = jwtService.extractEmail(token);
            boolean isAdmin = jwtService.isAdmin(token);

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Allow if admin or if user is requesting their own data
            if (!isAdmin && !user.getEmail().equals(email)) {
                return ResponseEntity.status(403).body("❌ Forbidden: You can only view your own profile");
            }

            // Return user without password
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("name", user.getName());
            userData.put("email", user.getEmail());
            userData.put("role", user.getRole().name());
            userData.put("active", user.isActive());

            System.out.println("✅ User found: " + id);
            return ResponseEntity.ok(userData);

        } catch (RuntimeException e) {
            System.err.println("❌ User not found: " + id);
            return ResponseEntity.status(404).body("❌ " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error fetching user: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Failed to fetch user: " + e.getMessage());
        }
    }
}