package com.smart_task.smart_task_manager_backend.Controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smart_task.smart_task_manager_backend.Model.User;
import com.smart_task.smart_task_manager_backend.Repository.UserRepository;
import com.smart_task.smart_task_manager_backend.Service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/signup")
    public TokenResponse signup(@RequestBody SignupRequest req) {
        String token = authService.signup(req.getName(), req.getEmail(), req.getPassword(), req.isAdmin());
        return new TokenResponse(token);
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest req) {
        String token = authService.login(req.getEmail(), req.getPassword());
        return new TokenResponse(token);
    }

    // ✅ Get all users (for admin)
 // ✅ Get all NON-ADMIN users (for admin view)
    @GetMapping("/admin/users")
    public List<User> getAllNonAdminUsers() {
        return userRepository.findByRoleNot(com.smart_task.smart_task_manager_backend.Model.Role.ADMIN);
    }


    // ✅ Activate / Deactivate user
    @PutMapping("/admin/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(active);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    // DTOs
    static class SignupRequest {
        private String name;
        private String email;
        private String password;
        @JsonProperty("isAdmin")
        private boolean isAdmin;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public boolean isAdmin() { return isAdmin; }
        public void setAdmin(boolean admin) { isAdmin = admin; }
    }

    static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    static class TokenResponse {
        private final String token;
        public TokenResponse(String token) { this.token = token; }
        public String getToken() { return token; }
    }
}
