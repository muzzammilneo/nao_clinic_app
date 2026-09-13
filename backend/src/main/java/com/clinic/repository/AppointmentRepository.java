package com.clinic.repository;

import com.clinic.dto.AppointmentDetailsDTO;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AppointmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public AppointmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Appointment> appointmentRowMapper = (rs, rowNum) -> new Appointment(
            rs.getLong("id"),
            rs.getString("patient_name"),
            rs.getString("patient_phone"),
            rs.getLong("doctor_id"),
            rs.getDate("appointment_date").toLocalDate(),
            rs.getTime("appointment_time").toLocalTime(),
            AppointmentStatus.valueOf(rs.getString("status"))
    );

    private final RowMapper<AppointmentDetailsDTO> detailsRowMapper = (rs, rowNum) -> new AppointmentDetailsDTO(
            rs.getLong("id"),
            rs.getString("patient_name"),
            rs.getString("patient_phone"),
            rs.getLong("doctor_id"),
            rs.getString("doctor_name"),
            rs.getString("doctor_specialization"),
            rs.getDate("appointment_date").toLocalDate(),
            rs.getTime("appointment_time").toLocalTime(),
            AppointmentStatus.valueOf(rs.getString("status"))
    );

    public Appointment save(Appointment appointment) {
        String sql = "INSERT INTO appointments (patient_name, patient_phone, doctor_id, appointment_date, appointment_time, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, appointment.getPatientName());
            ps.setString(2, appointment.getPatientPhone());
            ps.setLong(3, appointment.getDoctorId());
            ps.setDate(4, Date.valueOf(appointment.getAppointmentDate()));
            ps.setTime(5, Time.valueOf(appointment.getAppointmentTime()));
            ps.setString(6, appointment.getStatus().name());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            appointment.setId(key.longValue());
        }
        return appointment;
    }

    public Optional<Appointment> findById(Long id) {
        String sql = "SELECT id, patient_name, patient_phone, doctor_id, appointment_date, appointment_time, status " +
                     "FROM appointments WHERE id = ?";
        try {
            Appointment appt = jdbcTemplate.queryForObject(sql, appointmentRowMapper, id);
            return Optional.ofNullable(appt);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<AppointmentDetailsDTO> findDetailsById(Long id) {
        String sql = "SELECT a.id, a.patient_name, a.patient_phone, a.doctor_id, " +
                     "d.name AS doctor_name, d.specialization AS doctor_specialization, " +
                     "a.appointment_date, a.appointment_time, a.status " +
                     "FROM appointments a " +
                     "LEFT JOIN doctors d ON a.doctor_id = d.id " +
                     "WHERE a.id = ?";
        try {
            AppointmentDetailsDTO dto = jdbcTemplate.queryForObject(sql, detailsRowMapper, id);
            return Optional.ofNullable(dto);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<AppointmentDetailsDTO> findDetailsByPhoneAndId(String phone, Long id) {
        String sql = "SELECT a.id, a.patient_name, a.patient_phone, a.doctor_id, " +
                     "d.name AS doctor_name, d.specialization AS doctor_specialization, " +
                     "a.appointment_date, a.appointment_time, a.status " +
                     "FROM appointments a " +
                     "LEFT JOIN doctors d ON a.doctor_id = d.id " +
                     "WHERE a.id = ? AND a.patient_phone = ?";
        try {
            AppointmentDetailsDTO dto = jdbcTemplate.queryForObject(sql, detailsRowMapper, id, phone);
            return Optional.ofNullable(dto);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<AppointmentDetailsDTO> findAllFiltered(Long doctorId, LocalDate date) {
        StringBuilder sql = new StringBuilder(
                "SELECT a.id, a.patient_name, a.patient_phone, a.doctor_id, " +
                "d.name AS doctor_name, d.specialization AS doctor_specialization, " +
                "a.appointment_date, a.appointment_time, a.status " +
                "FROM appointments a " +
                "LEFT JOIN doctors d ON a.doctor_id = d.id " +
                "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (doctorId != null) {
            sql.append("AND a.doctor_id = ? ");
            params.add(doctorId);
        }
        if (date != null) {
            sql.append("AND a.appointment_date = ? ");
            params.add(Date.valueOf(date));
        }

        sql.append("ORDER BY a.appointment_date DESC, a.appointment_time ASC");
        return jdbcTemplate.query(sql.toString(), detailsRowMapper, params.toArray());
    }

    public boolean existsConflict(Long doctorId, LocalDate date, LocalTime time, Long excludeAppointmentId) {
        StringBuilder sql = new StringBuilder(
                "SELECT count(*) FROM appointments " +
                "WHERE doctor_id = ? AND appointment_date = ? AND appointment_time = ? " +
                "AND status IN ('PENDING', 'APPROVED') "
        );
        List<Object> params = new ArrayList<>();
        params.add(doctorId);
        params.add(Date.valueOf(date));
        params.add(Time.valueOf(time));

        if (excludeAppointmentId != null) {
            sql.append("AND id <> ?");
            params.add(excludeAppointmentId);
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null && count > 0;
    }

    public void updateStatusAndSchedule(Long id, AppointmentStatus status, LocalDate date, LocalTime time) {
        StringBuilder sql = new StringBuilder("UPDATE appointments SET status = ? ");
        List<Object> params = new ArrayList<>();
        params.add(status.name());

        if (date != null) {
            sql.append(", appointment_date = ? ");
            params.add(Date.valueOf(date));
        }
        if (time != null) {
            sql.append(", appointment_time = ? ");
            params.add(Time.valueOf(time));
        }

        sql.append("WHERE id = ?");
        params.add(id);

        jdbcTemplate.update(sql.toString(), params.toArray());
    }
}
