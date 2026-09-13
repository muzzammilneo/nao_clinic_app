package com.clinic.service;

import com.clinic.exception.UnauthorizedException;
import com.clinic.model.Admin;
import com.clinic.repository.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Admin authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            throw new UnauthorizedException("Username and password must be provided.");
        }

        Admin admin = adminRepository.findByUsername(username.trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password."));

        if (!passwordEncoder.matches(rawPassword, admin.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password.");
        }

        return admin;
    }
}
