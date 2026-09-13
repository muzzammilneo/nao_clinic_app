package com.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public class DoctorRequest {

    @NotBlank(message = "Doctor name is required")
    private String name;

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @NotBlank(message = "Available days are required (e.g. Monday,Wednesday,Friday)")
    private String availableDays;

    public DoctorRequest() {
    }

    public DoctorRequest(String name, String specialization, String availableDays) {
        this.name = name;
        this.specialization = specialization;
        this.availableDays = availableDays;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(String availableDays) {
        this.availableDays = availableDays;
    }
}
