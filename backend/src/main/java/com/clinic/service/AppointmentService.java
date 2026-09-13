package com.clinic.service;

import com.clinic.dto.AppointmentDetailsDTO;
import com.clinic.dto.AppointmentUpdateRequest;
import com.clinic.dto.BookingRequest;
import com.clinic.exception.ConflictException;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.exception.ValidationException;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;
import com.clinic.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{7,20}$");

    public AppointmentService(AppointmentRepository appointmentRepository, DoctorService doctorService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorService = doctorService;
    }

    public AppointmentDetailsDTO bookAppointment(BookingRequest request) {
        validateBookingRequest(request);

        Doctor doctor = doctorService.getDoctorById(request.getDoctorId());
        validateDoctorAvailability(doctor, request.getAppointmentDate());

        // Check double booking
        if (appointmentRepository.existsConflict(request.getDoctorId(), request.getAppointmentDate(), request.getAppointmentTime(), null)) {
            throw new ConflictException("Doctor " + doctor.getName() + " already has an appointment scheduled at " +
                    request.getAppointmentTime() + " on " + request.getAppointmentDate() + ". Please select another time slot.");
        }

        Appointment appointment = new Appointment(
                null,
                request.getPatientName().trim(),
                request.getPatientPhone().trim(),
                request.getDoctorId(),
                request.getAppointmentDate(),
                request.getAppointmentTime(),
                AppointmentStatus.PENDING
        );

        Appointment saved = appointmentRepository.save(appointment);
        return appointmentRepository.findDetailsById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Could not retrieve created appointment details"));
    }

    public AppointmentDetailsDTO lookupPatientAppointment(String phone, Long id) {
        if (phone == null || phone.trim().isEmpty() || id == null) {
            throw new ValidationException("Both phone number and appointment ID are required for lookup.");
        }
        return appointmentRepository.findDetailsByPhoneAndId(phone.trim(), id)
                .orElseThrow(() -> new ResourceNotFoundException("No appointment found matching ID #" + id + " with phone number: " + phone));
    }

    public AppointmentDetailsDTO cancelPatientAppointment(Long id, String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new ValidationException("Phone number is required to cancel an appointment.");
        }

        AppointmentDetailsDTO current = lookupPatientAppointment(phone, id);
        if (current.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ValidationException("This appointment is already cancelled.");
        }

        appointmentRepository.updateStatusAndSchedule(id, AppointmentStatus.CANCELLED, null, null);
        return appointmentRepository.findDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found after cancellation."));
    }

    public List<AppointmentDetailsDTO> getAppointmentsForAdmin(Long doctorId, LocalDate date) {
        return appointmentRepository.findAllFiltered(doctorId, date);
    }

    public AppointmentDetailsDTO updateAppointmentByAdmin(Long id, AppointmentUpdateRequest request) {
        Appointment existing = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        LocalDate targetDate = request.getAppointmentDate() != null ? request.getAppointmentDate() : existing.getAppointmentDate();
        LocalTime targetTime = request.getAppointmentTime() != null ? request.getAppointmentTime() : existing.getAppointmentTime();
        AppointmentStatus targetStatus = request.getStatus() != null ? request.getStatus() : existing.getStatus();

        // If date or time is modified or if approving, verify conflict
        if (!targetDate.equals(existing.getAppointmentDate()) || !targetTime.equals(existing.getAppointmentTime())) {
            if (targetDate.isBefore(LocalDate.now())) {
                throw new ValidationException("Rescheduled date cannot be in the past.");
            }

            Doctor doctor = doctorService.getDoctorById(existing.getDoctorId());
            validateDoctorAvailability(doctor, targetDate);

            if (appointmentRepository.existsConflict(existing.getDoctorId(), targetDate, targetTime, id)) {
                throw new ConflictException("Cannot reschedule: Doctor already has an active appointment at " + targetTime + " on " + targetDate);
            }
        }

        appointmentRepository.updateStatusAndSchedule(id, targetStatus, targetDate, targetTime);
        return appointmentRepository.findDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found after update."));
    }

    private void validateBookingRequest(BookingRequest request) {
        if (request.getPatientName() == null || request.getPatientName().trim().isEmpty()) {
            throw new ValidationException("Patient name cannot be empty.");
        }
        if (request.getPatientPhone() == null || !PHONE_PATTERN.matcher(request.getPatientPhone().trim()).matches()) {
            throw new ValidationException("A valid phone number (7-20 digits) is required.");
        }
        if (request.getDoctorId() == null) {
            throw new ValidationException("Doctor ID must be specified.");
        }
        if (request.getAppointmentDate() == null) {
            throw new ValidationException("Appointment date must be specified.");
        }
        if (request.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new ValidationException("Appointment date cannot be in the past.");
        }
        if (request.getAppointmentTime() == null) {
            throw new ValidationException("Appointment time must be specified.");
        }
    }

    private void validateDoctorAvailability(Doctor doctor, LocalDate date) {
        String availableDays = doctor.getAvailableDays();
        if (availableDays == null || availableDays.trim().isEmpty()) {
            return; // No restrictions specified
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String fullDay = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        String shortDay = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toLowerCase();

        boolean isAvailable = Arrays.stream(availableDays.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(day -> day.contains(shortDay) || fullDay.contains(day));

        if (!isAvailable) {
            throw new ValidationException(doctor.getName() + " is not available on " +
                    dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) +
                    ". Available days: " + availableDays);
        }
    }
}
