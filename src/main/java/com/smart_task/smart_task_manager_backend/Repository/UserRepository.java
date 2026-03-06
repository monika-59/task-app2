package com.smart_task.smart_task_manager_backend.Repository;

import com.smart_task.smart_task_manager_backend.Model.Role;
import com.smart_task.smart_task_manager_backend.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ✅ Find user by email
    Optional<User> findByEmail(String email);
    
    // ✅ Check if email exists
    boolean existsByEmail(String email);
    
    // ✅ Count users by role (for admin limit check)
    long countByRole(Role role);
    
    // ✅ Find all users except those with a specific role (to exclude admins)
    List<User> findByRoleNot(Role role);
    
    // ✅ Find all users by role
    List<User> findByRole(Role role);
    
    // ✅ Find all active users
    List<User> findByActiveTrue();
    
    // ✅ Find all inactive users
    List<User> findByActiveFalse();
}
