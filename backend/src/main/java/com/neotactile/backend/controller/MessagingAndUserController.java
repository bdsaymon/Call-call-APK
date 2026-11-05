package com.neotactile.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MessagingAndUserController {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${webrtc.stun-url}")
    private String stunUrl;

    @Value("${webrtc.turn-url}")
    private String turnUrl;

    @Value("${webrtc.turn-username}")
    private String turnUsername;

    public MessagingAndUserController(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<Map<String, Object>> registerEmailAccount(@RequestBody Map<String, String> body) {
        String uid = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String email = body.getOrDefault("email", "").trim();
        String username = body.getOrDefault("username", "").trim().replace("@", "");
        String displayName = body.getOrDefault("displayName", username).trim();
        String rawPassword = body.getOrDefault("password", "");
        String shortCode = "NT-" + (Math.abs(uid.hashCode()) % 9000 + 1000);

        jdbcTemplate.update(
            "INSERT INTO users (uid, email, username, password_hash, auth_provider, display_name, short_code) VALUES (?, ?, ?, ?, 'EMAIL', ?, ?)",
            uid, email, username, passwordEncoder.encode(rawPassword), displayName, shortCode
        );

        return ResponseEntity.ok(Map.of(
            "uid", uid,
            "email", email,
            "username", username,
            "displayName", displayName,
            "shortCode", shortCode
        ));
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(@RequestParam("q") String query) {
        String pattern = "%" + query.trim().replace("@", "") + "%";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT uid, username, display_name, short_code, bio, is_online, last_seen FROM users " +
            "WHERE is_private = FALSE AND (username ILIKE ? OR display_name ILIKE ? OR email ILIKE ? OR phone_number ILIKE ? OR short_code ILIKE ?) LIMIT 30",
            pattern, pattern, pattern, pattern, pattern
        );
        return ResponseEntity.ok(results);
    }

    @PostMapping("/contacts/request")
    public ResponseEntity<Map<String, Object>> addOrRequestContact(@RequestBody Map<String, String> body) {
        String ownerUid = body.get("ownerUid");
        String contactUid = body.get("contactUid");
        jdbcTemplate.update(
            "INSERT INTO user_contacts (owner_uid, contact_uid, status) VALUES (?, ?, 'ACCEPTED') " +
            "ON CONFLICT (owner_uid, contact_uid) DO UPDATE SET status = 'ACCEPTED'",
            ownerUid, contactUid
        );
        return ResponseEntity.ok(Map.of("status", "ACCEPTED", "contactUid", contactUid));
    }

    @GetMapping("/webrtc/ice-config")
    public ResponseEntity<Map<String, Object>> getWebRtcIceServers() {
        return ResponseEntity.ok(Map.of(
            "stunServer", stunUrl,
            "turnServer", turnUrl,
            "turnUsername", turnUsername
        ));
    }
}
