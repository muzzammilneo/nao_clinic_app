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
        // 1. Initialize default admin user if absent
        if (adminRepository.count() == 0) {
            String defaultUsername = "admin";
            String defaultPassword = "admin123";
            String hashedPassword = passwordEncoder.encode(defaultPassword);
            adminRepository.save(new Admin(null, defaultUsername, hashedPassword));
            log.info("Initialized default admin user: '{}' (password: '{}')", defaultUsername, defaultPassword);
        }

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
