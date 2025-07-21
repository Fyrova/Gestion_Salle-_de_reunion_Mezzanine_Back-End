package edbm.salle.demo.service;

import edbm.salle.demo.model.Reservation;

import java.time.LocalDateTime;
import java.util.List;

public interface IReservationService {

    List<Reservation> findAll();

    List<Reservation> findReservationsForReminder(LocalDateTime from, LocalDateTime to);

    Reservation save(Reservation reservation) throws IllegalArgumentException;

    Reservation update(Reservation updatedReservation) throws IllegalArgumentException;

    void cancel(Long id);

    void delete(Long id);
}
