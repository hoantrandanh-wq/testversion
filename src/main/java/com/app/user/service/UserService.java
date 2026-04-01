package com.app.user.service;

import com.app.common.util.SecurityUtil;
import com.app.user.model.User;
import com.app.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(u -> SecurityUtil.verify(password, u.getPassword()))
                .orElse(null);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User create(User user) {
        String username = normalizeUsername(user.getUsername());

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new RuntimeException("Password is required");
        }

        if (user.getRole() == null) {
            throw new RuntimeException("Role is required");
        }

        user.setUsername(username);
        user.setPassword(SecurityUtil.hash(user.getPassword()));

        User saved = userRepository.save(user);
        log.info("User created: '{}'", username);

        return saved;
    }

    public void update(User user) {
        User old = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String username = normalizeUsername(user.getUsername());
        if (!old.getUsername().equals(username)) {
            throw new RuntimeException("Cannot change username");
        }

        if (user.getRole() == null) {
            throw new RuntimeException("Role is required");
        }

        old.setRole(user.getRole());

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            old.setPassword(SecurityUtil.hash(user.getPassword()));
        }

        userRepository.save(old);
        log.info("User updated: '{}'", username);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
        log.info("User deleted: id={}", id);
    }

    private String normalizeUsername(String username) {
        if (username == null) return null;
        return username.trim().toLowerCase();
    }
}