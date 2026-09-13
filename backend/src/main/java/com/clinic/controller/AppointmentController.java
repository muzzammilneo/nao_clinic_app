package com.clinic.controller;

import com.clinic.dto.AppointmentDetailsDTO;
import com.clinic.dto.BookingRequest;
import com.clinic.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentDetailsDTO> bookAppointment(@Valid @RequestBody BookingRequest request) {
        AppointmentDetailsDTO created = appointmentService.bookAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<AppointmentDetailsDTO> lookupAppointment(
            @RequestParam("phone") String phone,
            @RequestParam("id") Long id) {
        AppointmentDetailsDTO appt = appointmentService.lookupPatientAppointment(phone, id);
        return ResponseEntity.ok(appt);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<AppointmentDetailsDTO> cancelAppointment(
            @PathVariable("id") Long id,
            @RequestParam(value = "phone", required = false) String queryPhone,
            @RequestBody(required = false) Map<String, String> body) {
        String phone = queryPhone;
        if (phone == null && body != null) {
            phone = body.get("phone");
        }
        AppointmentDetailsDTO cancelled = appointmentService.cancelPatientAppointment(id, phone);
        return ResponseEntity.ok(cancelled);
    }
}
