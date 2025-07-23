package edbm.salle.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Reservation {

    public enum Status {
        CONFIRMED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;

    private String subject;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "organizer_id")
    private User organizer;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "reservation_type")
    private String reservationType;

    @Column(name = "equipment")
    private String equipment;

    @Column(name = "disposition")
    private String disposition;

    @Column(name = "participants_count")
    private Integer participantsCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    // New fields for recurrence support
    @Column(name = "recurrence_rule")
    private String recurrenceRule;

    @ManyToOne
    @JoinColumn(name = "parent_reservation_id")
    private Reservation parentReservation;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "departement")
    private String departement;
}
