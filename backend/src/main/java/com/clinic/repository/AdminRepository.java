package com.clinic.repository;

import com.clinic.model.Admin;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class AdminRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Admin> adminRowMapper = (rs, rowNum) -> new Admin(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash")
    );

    public Optional<Admin> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash FROM admins WHERE username = ?";
        try {
            Admin admin = jdbcTemplate.queryForObject(sql, adminRowMapper, username);
            return Optional.ofNullable(admin);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Admin save(Admin admin) {
        String sql = "INSERT INTO admins (username, password_hash) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, admin.getUsername());
            ps.setString(2, admin.getPasswordHash());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            admin.setId(key.longValue());
        }
        return admin;
    }

    public void update(Admin admin) {
        String sql = "UPDATE admins SET password_hash = ? WHERE id = ?";
        jdbcTemplate.update(sql, admin.getPasswordHash(), admin.getId());
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM admins", Long.class);
        return count != null ? count : 0;
    }
}
