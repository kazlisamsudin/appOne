package com.app.ai.service;

import com.app.ai.model.User;
import com.app.ai.model.Role;
import com.app.ai.repository.UserRepository;
import com.app.ai.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("=== UserService: loadUserByUsername called with: " + username + " ===");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("=== UserService: User not found: " + username + " ===");
                    return new UsernameNotFoundException("User not found: " + username);
                });

        System.out.println("=== UserService: Found user: " + user.getUsername() + " ===");
        System.out.println("=== UserService: User enabled: " + user.isEnabled() + " ===");
        System.out.println("=== UserService: User roles count: " + user.getRoles().size() + " ===");

        String[] authorities = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName())
                .toArray(String[]::new);

        System.out.println("=== UserService: User authorities: " + String.join(", ", authorities) + " ===");

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();

        System.out.println("=== UserService: Created UserDetails for: " + userDetails.getUsername() + " ===");
        return userDetails;
    }

    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> findPaginatedUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User registerUser(User user) {
        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set default role if no roles are assigned
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("User role not found"));
            user.setRoles(Set.of(userRole));
        }

        return userRepository.save(user);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public void updateUserPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public boolean verifyCurrentPassword(User user, String currentPassword) {
        return passwordEncoder.matches(currentPassword, user.getPassword());
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }
}
