package com.app.user.repository;

import com.app.common.enums.Role;
import com.app.user.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private User mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRole(Role.valueOf(rs.getString("role")));
        return u;
    }

    public List<User> findAll() {
        String sql = "SELECT id, username, password, role FROM user";

        return jdbcTemplate.query(sql, this::mapRow);
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT id, username, password, role FROM user WHERE id = ?";

        try {
            User user = jdbcTemplate.queryForObject(sql, this::mapRow, id);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password, role FROM user WHERE username = ?";

        try {
            User user = jdbcTemplate.queryForObject(sql, this::mapRow, username);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findUserByUserName(String username) {
        return findByUsername(username);
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM user WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count > 0;
    }

    public User save(User user) {
        if (user.getId() == null) {
            String sql = "INSERT INTO user(username, password, role) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql,
                    user.getUsername(),
                    user.getPassword(),
                    user.getRole().name()
            );

            Long id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
            user.setId(id);

        } else {
            String sql = "UPDATE user SET password = ?, role = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    user.getPassword(),
                    user.getRole().name(),
                    user.getId()
            );
        }

        return user;
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM user WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}