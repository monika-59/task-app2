package com.smart_task.smart_task_manager_backend.Security;

import com.smart_task.smart_task_manager_backend.Model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("❌ Token expired");
        } catch (UnsupportedJwtException e) {
            System.out.println("❌ Unsupported token");
        } catch (MalformedJwtException e) {
            System.out.println("❌ Malformed token");
        } catch (SecurityException e) {
            System.out.println("❌ Invalid signature");
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Token is empty or null");
        }
        return false;
    }

    // ✅ Extract email from token using generic extractClaim method
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ✅ Extract role from token using generic extractClaim method
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    // ✅ Check if user is admin
    public boolean isAdmin(String token) {
        try {
            String role = extractRole(token);
            return "ADMIN".equals(role);
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ Generic method to extract any claim from token
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ✅ Extract all claims from token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ✅ Backward compatibility - parseClaims uses extractAllClaims
    private Claims parseClaims(String token) {
        return extractAllClaims(token);
    }
}
