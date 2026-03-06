package com.smart_task.smart_task_manager_backend.Service;

import com.smart_task.smart_task_manager_backend.Model.Role;
import com.smart_task.smart_task_manager_backend.Model.User;
import com.smart_task.smart_task_manager_backend.Repository.UserRepository;
import com.smart_task.smart_task_manager_backend.Security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String signup(String name, String email, String password, boolean isAdmin) {
        System.out.println(">>> [AuthService] signup called with params:");
        System.out.println("    name: " + name);
        System.out.println("    email: " + email);
        System.out.println("    isAdmin: " + isAdmin);

        if (isAdmin && userRepository.countByRole(Role.ADMIN) >= 10) {
            throw new RuntimeException("Max 10 admins allowed");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        Role roleToSet = isAdmin ? Role.ADMIN : Role.USER;
        System.out.println(">>> [AuthService] Role being set: " + roleToSet);

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(roleToSet);

        User savedUser = userRepository.save(user);
        System.out.println(">>> [AuthService] User saved with role: " + savedUser.getRole());

        return jwtService.generateToken(savedUser);
    }

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.isActive()) {
            throw new RuntimeException("Your account has been deactivated. Please contact admin.");
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtService.generateToken(user);
    }
}
