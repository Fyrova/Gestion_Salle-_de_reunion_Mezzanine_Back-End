package edbm.salle.demo.dto;

import edbm.salle.demo.model.Reservation;

import java.time.LocalDate;
import java.time.LocalTime;

public class ReservationDTO {

    private Long id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String subject;
    private String organizerName;
    private String status;

    public ReservationDTO(Reservation reservation) {
        this.id = reservation.getId();
        this.date = reservation.getDate();
        this.startTime = reservation.getStartTime();
        this.endTime = reservation.getEndTime();
        this.subject = reservation.getSubject();
        this.organizerName = reservation.getOrganizer() != null ? reservation.getOrganizer().getName() : null;
        this.status = reservation.getStatus() != null ? reservation.getStatus().name() : null;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
