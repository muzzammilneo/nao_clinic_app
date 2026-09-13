package com.clinic.service;

import com.clinic.dto.DoctorRequest;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.exception.ValidationException;
import com.clinic.model.Doctor;
import com.clinic.repository.DoctorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Doctor getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));
    }

    public Doctor createDoctor(DoctorRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Doctor name is required");
        }
        Doctor doctor = new Doctor(
                null,
                request.getName().trim(),
                request.getSpecialization() != null ? request.getSpecialization().trim() : "",
                request.getAvailableDays() != null ? request.getAvailableDays().trim() : ""
        );
        return doctorRepository.save(doctor);
    }

    public Doctor updateDoctor(Long id, DoctorRequest request) {
        Doctor existing = getDoctorById(id);
        existing.setName(request.getName().trim());
        existing.setSpecialization(request.getSpecialization() != null ? request.getSpecialization().trim() : "");
        existing.setAvailableDays(request.getAvailableDays() != null ? request.getAvailableDays().trim() : "");
        doctorRepository.update(existing);
        return existing;
    }

    public void deleteDoctor(Long id) {
        getDoctorById(id); // validates existence
        doctorRepository.deleteById(id);
    }
}
