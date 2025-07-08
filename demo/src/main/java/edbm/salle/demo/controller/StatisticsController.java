package edbm.salle.demo.controller;

import edbm.salle.demo.model.Reservation;
import edbm.salle.demo.repository.ReservationRepository;
import edbm.salle.demo.dto.ReservationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private ReservationRepository reservationRepository;

    @GetMapping
    public Map<String, Object> getStatistics() {
        long totalReservations = reservationRepository.count();

        LocalDate today = LocalDate.now();
        long reservationsToday = reservationRepository.countByDate(today);
        Long participantsToday = reservationRepository.countParticipantsByDate(today);
        if (participantsToday == null) participantsToday = 0L;
        Long reservedMinutesToday = reservationRepository.sumReservedMinutesByDate(today);
        if (reservedMinutesToday == null) reservedMinutesToday = 0L;
        List<Reservation> reservationsListToday = reservationRepository.findReservationsByDate(today);

        // Ensure stats are zero if no reservations
        if (reservationsToday == 0) {
            participantsToday = 0L;
            reservedMinutesToday = 0L;
            reservationsListToday = List.of();
        }

        java.time.LocalDateTime startOfDay = java.time.LocalDateTime.of(today, java.time.LocalTime.MIN);
        java.time.LocalDateTime endOfDay = java.time.LocalDateTime.of(today, java.time.LocalTime.MAX);
        List<Reservation> reservationsCreatedToday = reservationRepository.findReservationsCreatedOnDate(startOfDay, endOfDay);
        if (reservationsCreatedToday == null) {
            reservationsCreatedToday = List.of();
        }

        List<ReservationDTO> reservationsDTOList = reservationsListToday.stream()
                .map(ReservationDTO::new)
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReservations", totalReservations);
        stats.put("reservationsToday", reservationsToday);
        stats.put("participantsToday", participantsToday);
        stats.put("reservedMinutesToday", reservedMinutesToday);
        stats.put("reservationsListToday", reservationsDTOList);
        stats.put("reservationsCreatedToday", reservationsCreatedToday);

        return stats;
    }
}
