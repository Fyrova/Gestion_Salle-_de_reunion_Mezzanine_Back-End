package edbm.salle.demo.model;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

public class AuditListener {

    @PrePersist
    public void setCreatedAt(Object entity) {
        if (entity instanceof Reservation) {
            Reservation reservation = (Reservation) entity;
            LocalDateTime now = LocalDateTime.now();
            if (reservation.getCreatedAt() == null) {
                reservation.setCreatedAt(now);
            }
            reservation.setUpdatedAt(now);
        }
    }

    @PreUpdate
    public void setUpdatedAt(Object entity) {
        if (entity instanceof Reservation) {
            Reservation reservation = (Reservation) entity;
            reservation.setUpdatedAt(LocalDateTime.now());
        }
    }
}
