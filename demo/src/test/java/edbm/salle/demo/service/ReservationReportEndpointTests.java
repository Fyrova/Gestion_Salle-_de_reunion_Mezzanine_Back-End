package edbm.salle.demo.service;

import edbm.salle.demo.model.Reservation;
import edbm.salle.demo.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReservationReportEndpointTests {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Reservation reservation;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        reservation = new Reservation();
        reservation.setId(1L);
        reservation.setDate(LocalDate.now());
        reservation.setStartTime(LocalTime.of(9, 0));
        reservation.setEndTime(LocalTime.of(10, 0));
        reservation.setSubject("Test Meeting");
    }

    @Test
    public void testGetReportData() {
        List<Reservation> mockReservations = Arrays.asList(reservation);
        when(reservationRepository.findAll()).thenReturn(mockReservations);

        List<Reservation> reportData = reservationService.findAll(); // Replace with actual report method if exists

        assertNotNull(reportData);
        assertEquals(1, reportData.size());
        assertEquals("Test Meeting", reportData.get(0).getSubject());
    }
}
