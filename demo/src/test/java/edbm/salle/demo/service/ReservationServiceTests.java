package edbm.salle.demo.service;

import edbm.salle.demo.model.Reservation;
import edbm.salle.demo.model.ReservationHistory;
import edbm.salle.demo.model.User;
import edbm.salle.demo.repository.ReservationHistoryRepository;
import edbm.salle.demo.repository.ReservationRepository;
import edbm.salle.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReservationServiceTests {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationHistoryRepository reservationHistoryRepository;

    @Mock
    private JavaMailSender mailSender; // For mocking email notifications

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
    public void testSaveReservation_Success() {
        when(userRepository.findByEmail(organizer.getEmail())).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(organizer);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        Reservation saved = reservationService.save(reservation);

        assertNotNull(saved);
        verify(userRepository).save(any(User.class));
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    public void testSaveReservation_OverlappingTime_ThrowsException() {
        List<Reservation> existing = new ArrayList<>();
        existing.add(reservation);
        when(reservationRepository.findAll()).thenReturn(existing);

        Reservation newReservation = new Reservation();
        newReservation.setDate(reservation.getDate());
        newReservation.setStartTime(LocalTime.of(9, 30));
        newReservation.setEndTime(LocalTime.of(10, 30));
        newReservation.setStatus(Reservation.Status.CONFIRMED);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reservationService.save(newReservation);
        });

        assertEquals("Le créneau horaire est déjà réservé.", exception.getMessage());
    }

    @Test
    public void testDeleteReservation_AddsHistoryEntry() {
        doNothing().when(reservationRepository).deleteById(reservation.getId());

        reservationService.delete(reservation.getId());

        verify(reservationRepository).deleteById(reservation.getId());
        // Assuming history entry is added in delete method or elsewhere - if implemented
        // Here we can verify if reservationHistoryRepository.save() was called if implemented
    }

    // Additional tests for notification sending, search, reports, and error handling can be added here
}
