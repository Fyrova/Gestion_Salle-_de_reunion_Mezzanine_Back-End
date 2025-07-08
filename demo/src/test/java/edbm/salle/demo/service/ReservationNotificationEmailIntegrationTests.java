package edbm.salle.demo.service;

import edbm.salle.demo.model.Reservation;
import edbm.salle.demo.model.User;
import edbm.salle.demo.repository.ReservationRepository;
import edbm.salle.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReservationNotificationEmailIntegrationTests {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;

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
    public void testSendNotificationEmailOnSave() {
        when(userRepository.findByEmail(organizer.getEmail())).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(organizer);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        Reservation saved = reservationService.save(reservation);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mailCaptor.capture());

        SimpleMailMessage sentMessage = mailCaptor.getValue();
        assertEquals(organizer.getEmail(), sentMessage.getTo()[0]);
        assertTrue(sentMessage.getSubject().contains("Notification"));
        assertTrue(sentMessage.getText().contains("confirmée"));
    }
}
