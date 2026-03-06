package com.smart_task.smart_task_manager_backend.Config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final String SECRET_KEY = "byJnYS4T7JzzzxzJ6GVST8Zu4Lb93AVL";

    public String extractRole(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY.getBytes())
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class); // expects ADMIN/USER
    }

    public boolean isAdmin(String token) {
        String role = extractRole(token);
        return role != null && role.equalsIgnoreCase("ADMIN");
    }
}
