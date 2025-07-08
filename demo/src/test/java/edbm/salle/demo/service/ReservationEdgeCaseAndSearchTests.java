package edbm.salle.demo.service;

import edbm.salle.demo.model.Reservation;
import edbm.salle.demo.model.User;
import edbm.salle.demo.repository.ReservationRepository;
import edbm.salle.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReservationEdgeCaseAndSearchTests {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Reservation reservation;
    private User organizer;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        organizer = new User();
        organizer.setId(1L);
        organizer.setName("Test Organizer");
        organizer.setEmail("organizer@example.com");

        reservation = new Reservation();
        reservation.setId(1L);
        reservation.setDate(LocalDate.now());
        reservation.setStartTime(LocalTime.of(9, 0));
        reservation.setEndTime(LocalTime.of(10, 0));
        reservation.setSubject("Test Meeting");
        reservation.setOrganizer(organizer);
        reservation.setStatus(Reservation.Status.CONFIRMED);
    }

    @Test
    public void testSaveReservation_StartTimeAfterEndTime_ThrowsException() {
        reservation.setStartTime(LocalTime.of(11, 0));
        reservation.setEndTime(LocalTime.of(10, 0));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reservationService.save(reservation);
        });

        assertEquals("L'heure de début doit être avant l'heure de fin.", exception.getMessage());
    }

    @Test
    public void testSaveReservation_OutOfWorkingHours_ThrowsException() {
        reservation.setStartTime(LocalTime.of(7, 0));
        reservation.setEndTime(LocalTime.of(8, 0));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reservationService.save(reservation);
        });

        assertEquals("La réservation doit être dans les heures ouvrables (8h30 - 16h30).", exception.getMessage());
    }

    @Test
    public void testSearchReservationsByDate() {
        List<Reservation> allReservations = new ArrayList<>();
        allReservations.add(reservation);
        when(reservationRepository.findAll()).thenReturn(allReservations);

        List<Reservation> results = reservationRepository.findAll().stream()
                .filter(r -> r.getDate().equals(reservation.getDate()))
                .toList();

        assertFalse(results.isEmpty());
        assertEquals(reservation.getDate(), results.get(0).getDate());
    }

    @Test
    public void testSearchReservationsByOrganizer() {
        List<Reservation> allReservations = new ArrayList<>();
        allReservations.add(reservation);
        when(reservationRepository.findAll()).thenReturn(allReservations);

        List<Reservation> results = reservationRepository.findAll().stream()
                .filter(r -> r.getOrganizer() != null && r.getOrganizer().getEmail().equals(organizer.getEmail()))
                .toList();

        assertFalse(results.isEmpty());
        assertEquals(organizer.getEmail(), results.get(0).getOrganizer().getEmail());
    }
}
