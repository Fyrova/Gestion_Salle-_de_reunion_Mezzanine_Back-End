package edbm.salle.demo.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.Objects;

@Entity
@Table(name = "participants")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    @ManyToOne
    @JoinColumn(name = "reservation_id")
    @JsonBackReference
    private Reservation reservation;

    public Participant() {
    }

    public Participant(String name, String email, Reservation reservation) {
        this.name = name;
        this.email = email;
        this.reservation = reservation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Participant)) return false;
        Participant that = (Participant) o;
        if (this.id != null && that.id != null) {
            return Objects.equals(this.id, that.id);
        }
        return Objects.equals(this.name, that.name) && Objects.equals(this.email, that.email);
    }

    @Override
    public int hashCode() {
        if (this.id != null) {
            return Objects.hash(this.id);
        }
        return Objects.hash(this.name, this.email);
    }
}
