package com.clinic.repository;

import com.clinic.model.Doctor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class DoctorRepository {

    private final JdbcTemplate jdbcTemplate;

    public DoctorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Doctor> rowMapper = (rs, rowNum) -> new Doctor(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("specialization"),
            rs.getString("available_days")
    );

    public List<Doctor> findAll() {
        String sql = "SELECT id, name, specialization, available_days FROM doctors ORDER BY name ASC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Doctor> findById(Long id) {
        String sql = "SELECT id, name, specialization, available_days FROM doctors WHERE id = ?";
        try {
            Doctor doctor = jdbcTemplate.queryForObject(sql, rowMapper, id);
            return Optional.ofNullable(doctor);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Doctor save(Doctor doctor) {
        String sql = "INSERT INTO doctors (name, specialization, available_days) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, doctor.getName());
            ps.setString(2, doctor.getSpecialization());
            ps.setString(3, doctor.getAvailableDays());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            doctor.setId(key.longValue());
        }
        return doctor;
    }

    public void update(Doctor doctor) {
        String sql = "UPDATE doctors SET name = ?, specialization = ?, available_days = ? WHERE id = ?";
        jdbcTemplate.update(sql, doctor.getName(), doctor.getSpecialization(), doctor.getAvailableDays(), doctor.getId());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM doctors WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM doctors", Long.class);
        return count != null ? count : 0;
    }
}
