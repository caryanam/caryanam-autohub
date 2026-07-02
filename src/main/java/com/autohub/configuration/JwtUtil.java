package com.autohub.configuration;

import com.autohub.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    // SECRET KEY
    private static final String SECRET =
            "mysecretkeymysecretkeymysecretkey12345";

    // KEY
    private final Key key =
            Keys.hmacShaKeyFor(SECRET.getBytes());

    // GENERATE TOKEN
    public String generateToken(
            Long id,
            String name,
            String username,
            Role role,
            String mobileNumber,
            String city
            ) {

        return Jwts.builder()
                .setSubject(username)
                .setId(UUID.randomUUID().toString())
                .claim("id", id)
                .claim("name", name)
                .claim("role", role.name())
                .claim("mobileNumber",mobileNumber)
                .claim("city",city)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + 1000 * 60 * 60)
                )
                .signWith(key)
                .compact();
    }


    // EXTRACT USERNAME
    public String extractUsername(String token) {

        return getClaims(token).getSubject();
    }

    // EXTRACT ID
    public Long extractId(String token) {
        return getClaims(token).get("id", Long.class);
    }

    // EXTRACT NAME
    public String extractName(String token) {
        return getClaims(token).get("name", String.class);
    }

    // EXTRACT ROLE
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // VALIDATE TOKEN
    public boolean validateToken(
            String token,
            String username) {

        return username.equals(
                extractUsername(token)
        ) && !isTokenExpired(token);
    }

    // CHECK TOKEN EXPIRY
    private boolean isTokenExpired(String token) {

        return getClaims(token)
                .getExpiration()
                .before(new Date());
    }

    // GET CLAIMS
    private Claims getClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}