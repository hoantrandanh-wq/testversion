package com.app.repository;

import com.app.model.Greeting;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GreetingRepository {

    private final JdbcTemplate jdbcTemplate;

    public GreetingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Greeting> findAll() {
        String sql = "SELECT id, name FROM greeting";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Greeting g = new Greeting();
            g.setId(rs.getLong("id"));
            g.setName(rs.getString("name"));
            return g;
        });
    }
}