package com.clinic.controller;

import com.clinic.config.SessionAuthInterceptor;
import com.clinic.dto.AppointmentDetailsDTO;
import com.clinic.dto.AppointmentUpdateRequest;
import com.clinic.dto.DoctorRequest;
import com.clinic.dto.LoginRequest;
import com.clinic.model.Admin;
import com.clinic.model.Doctor;
import com.clinic.service.AdminService;
import com.clinic.service.AppointmentService;
import com.clinic.service.DoctorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AppointmentService appointmentService;
    private final DoctorService doctorService;

    public AdminController(AdminService adminService,
                           AppointmentService appointmentService,
                           DoctorService doctorService) {
        this.adminService = adminService;
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                    HttpServletRequest request) {
        Admin admin = adminService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

        // Create new session
        HttpSession session = request.getSession(true);
        session.setAttribute(SessionAuthInterceptor.ADMIN_SESSION_KEY, admin.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("username", admin.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String username = (session != null) ? (String) session.getAttribute(SessionAuthInterceptor.ADMIN_SESSION_KEY) : null;
        return ResponseEntity.ok(Map.of(
                "authenticated", username != null,
                "username", username != null ? username : ""
        ));
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentDetailsDTO>> getAppointments(
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AppointmentDetailsDTO> list = appointmentService.getAppointmentsForAdmin(doctorId, date);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/appointments/{id}")
    public ResponseEntity<AppointmentDetailsDTO> updateAppointment(
            @PathVariable("id") Long id,
            @RequestBody AppointmentUpdateRequest request) {
        AppointmentDetailsDTO updated = appointmentService.updateAppointmentByAdmin(id, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/doctors")
    public ResponseEntity<Doctor> addDoctor(@Valid @RequestBody DoctorRequest request) {
        Doctor created = doctorService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/doctors/{id}")
    public ResponseEntity<Doctor> editDoctor(
            @PathVariable("id") Long id,
            @Valid @RequestBody DoctorRequest request) {
        Doctor updated = doctorService.updateDoctor(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/doctors/{id}")
    public ResponseEntity<Map<String, String>> deleteDoctor(@PathVariable("id") Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.ok(Map.of("message", "Doctor removed successfully"));
    }
}
