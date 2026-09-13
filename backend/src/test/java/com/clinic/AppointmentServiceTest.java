package com.clinic;

import com.clinic.dto.AppointmentDetailsDTO;
import com.clinic.dto.BookingRequest;
import com.clinic.exception.ConflictException;
import com.clinic.exception.ValidationException;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;
import com.clinic.service.AppointmentService;
import com.clinic.service.DoctorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AppointmentServiceTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Doctor testDoctor;

    @BeforeEach
    void setup() {
        jdbcTemplate.execute("DELETE FROM appointments");
        testDoctor = doctorService.getAllDoctors().stream()
                .filter(d -> d.getAvailableDays().contains("Monday"))
                .findFirst()
                .orElseGet(() -> doctorService.getAllDoctors().get(0));
    }

    @Test
    void testBookAppointment_Success() {
        // Find next Monday
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalTime slotTime = LocalTime.of(10, 0);

        BookingRequest request = new BookingRequest(
                "John Doe",
                "555-0199",
                testDoctor.getId(),
                nextMonday,
                slotTime
        );

        AppointmentDetailsDTO dto = appointmentService.bookAppointment(request);
        assertNotNull(dto.getId());
        assertEquals("John Doe", dto.getPatientName());
        assertEquals(AppointmentStatus.PENDING, dto.getStatus());

        // Test lookup
        AppointmentDetailsDTO lookup = appointmentService.lookupPatientAppointment("555-0199", dto.getId());
        assertEquals(dto.getId(), lookup.getId());

        // Test cancel
        AppointmentDetailsDTO cancelled = appointmentService.cancelPatientAppointment(dto.getId(), "555-0199");
        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void testBookAppointment_DoubleBookingConflict() {
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalTime slotTime = LocalTime.of(11, 30);

        BookingRequest first = new BookingRequest(
                "Alice Wonder",
                "555-1111",
                testDoctor.getId(),
                nextMonday,
                slotTime
        );
        appointmentService.bookAppointment(first);

        // Attempt second booking for same doctor, date, and time
        BookingRequest duplicate = new BookingRequest(
                "Bob Builder",
                "555-2222",
                testDoctor.getId(),
                nextMonday,
                slotTime
        );

        assertThrows(ConflictException.class, () -> appointmentService.bookAppointment(duplicate));
    }

    @Test
    void testBookAppointment_PastDateRejection() {
        BookingRequest pastRequest = new BookingRequest(
                "Time Traveler",
                "555-3333",
                testDoctor.getId(),
                LocalDate.now().minusDays(1),
                LocalTime.of(10, 0)
        );

        assertThrows(ValidationException.class, () -> appointmentService.bookAppointment(pastRequest));
    }
}
