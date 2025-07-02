package com.app.ai.controller;

import com.app.ai.model.User;
import com.app.ai.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class DebugController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/debug/users")
    public String debugUsers() {
        List<User> users = userService.findAllUsers();
        StringBuilder sb = new StringBuilder();
        sb.append("=== DEBUG: All Users in Database ===\n");

        for (User user : users) {
            sb.append("Username: ").append(user.getUsername()).append("\n");
            sb.append("Email: ").append(user.getEmail()).append("\n");
            sb.append("Enabled: ").append(user.isEnabled()).append("\n");
            sb.append("Password starts with: ").append(user.getPassword().substring(0, Math.min(10, user.getPassword().length()))).append("...\n");
            sb.append("Password is BCrypt: ").append(user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$") || user.getPassword().startsWith("$2y$")).append("\n");
            sb.append("Roles: ").append(user.getRoles().stream().map(role -> role.getName()).collect(Collectors.joining(", "))).append("\n");

            // Test password matching
            boolean matches = passwordEncoder.matches("admin123", user.getPassword());
            sb.append("Password 'admin123' matches: ").append(matches).append("\n");

            sb.append("---\n");
        }

        return sb.toString();
    }

    @GetMapping("/debug/test-password")
    public String testPassword() {
        String plainText = "admin123";
        String encoded = passwordEncoder.encode(plainText);
        boolean matches = passwordEncoder.matches(plainText, encoded);

        return String.format(
            "Plain text: %s\nEncoded: %s\nMatches: %s",
            plainText, encoded, matches
        );
    }
}
