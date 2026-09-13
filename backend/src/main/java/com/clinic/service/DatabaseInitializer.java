package com.clinic.service;

import com.clinic.model.Admin;
import com.clinic.model.Doctor;
import com.clinic.repository.AdminRepository;
import com.clinic.repository.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(AdminRepository adminRepository,
                               DoctorRepository doctorRepository,
                               PasswordEncoder passwordEncoder,
                               org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        // 1. Resolve admin password: env var takes priority over hardcoded default
        String envPassword = System.getenv("ADMIN_PASSWORD");
        String adminPassword = (envPassword != null && !envPassword.isBlank()) ? envPassword : "Nao@Clinic#2025";

        // Force-reset admin credentials on every startup using raw JDBC
        String hashedPassword = passwordEncoder.encode(adminPassword);
        jdbcTemplate.update("DELETE FROM admins WHERE username = ?", "admin");
        jdbcTemplate.update("INSERT INTO admins (username, password_hash) VALUES (?, ?)", "admin", hashedPassword);
        log.info("Admin user 'admin' credentials reset on startup.");


        // 2. Initialize default sample doctors if table is empty
        if (doctorRepository.count() == 0) {
            doctorRepository.save(new Doctor(null, "Dr. Sarah Jenkins", "Cardiology", "Monday,Wednesday,Friday"));
            doctorRepository.save(new Doctor(null, "Dr. Marcus Vance", "General Medicine", "Monday,Tuesday,Wednesday,Thursday,Friday"));
            doctorRepository.save(new Doctor(null, "Dr. Priya Patel", "Pediatrics", "Tuesday,Thursday,Saturday"));
            doctorRepository.save(new Doctor(null, "Dr. Alan Ross", "Dermatology", "Monday,Wednesday,Saturday"));
            log.info("Initialized 4 sample doctors into database.");
        }

        // Resync identity sequences to prevent collisions
        try {
            jdbcTemplate.execute("ALTER TABLE doctors ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM doctors)");
            jdbcTemplate.execute("ALTER TABLE appointments ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM appointments)");
            jdbcTemplate.execute("ALTER TABLE admins ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM admins)");
        } catch (Exception e) {
            log.debug("Sequence restart not needed or supported: {}", e.getMessage());
        }
    }
}
