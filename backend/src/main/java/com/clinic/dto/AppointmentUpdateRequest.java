package com.clinic.dto;

import com.clinic.model.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentUpdateRequest {

    private AppointmentStatus status;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;

    public AppointmentUpdateRequest() {
    }

    public AppointmentUpdateRequest(AppointmentStatus status, LocalDate appointmentDate, LocalTime appointmentTime) {
        this.status = status;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }
}
