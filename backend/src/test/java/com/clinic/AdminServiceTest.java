package com.clinic;

import com.clinic.exception.UnauthorizedException;
import com.clinic.model.Admin;
import com.clinic.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @Test
    void testAdminAuth_Success() {
        Admin admin = adminService.authenticate("admin", "admin123");
        assertNotNull(admin);
        assertEquals("admin", admin.getUsername());
    }

    @Test
    void testAdminAuth_InvalidPassword() {
        assertThrows(UnauthorizedException.class, () -> adminService.authenticate("admin", "wrongpassword"));
    }

    @Test
    void testAdminAuth_NonExistentUser() {
        assertThrows(UnauthorizedException.class, () -> adminService.authenticate("nonexistent", "secret"));
    }
}
