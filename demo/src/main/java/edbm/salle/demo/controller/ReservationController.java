
package edbm.salle.demo.controller;

import edbm.salle.demo.model.Reservation;
import edbm.salle.demo.repository.ReservationRepository;
import edbm.salle.demo.service.ReservationService;
import edbm.salle.demo.service.RRuleHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.time.temporal.TemporalAdjusters;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ReservationController(ReservationRepository reservationRepository,
                                 ReservationService reservationService,
                                 ApplicationEventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping
     public List<Reservation> getReservationsByFilters(
            @RequestParam(required = false) String organizerName,
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String periodType) {

        List<Reservation> reservations;

        if (organizerName != null && !organizerName.isEmpty()) {
            if ("ordinary".equalsIgnoreCase(filterType)) {
                // Fetch ordinary and simple reservations by organizer (not recurring)
                reservations = reservationRepository.findByOrganizerNameContainingIgnoreCase(organizerName);
            } else {
                // Default or "recurring" filterType: fetch recurring reservations by organizer
                reservations = reservationRepository.findByOrganizer(organizerName);
            }
        } else if (periodType != null && date != null && !date.isEmpty()) {
            // New filtering by period type (day, week, month)
            LocalDate selectedDate = LocalDate.parse(date);
            LocalDate startDate, endDate;
            
            switch (periodType.toLowerCase()) {
                case "week":
                    // Get the week containing the selected date (Monday to Sunday)
                    startDate = selectedDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                    endDate = selectedDate.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
                    break;
                case "month":
                    // Get the entire month containing the selected date
                    startDate = selectedDate.with(TemporalAdjusters.firstDayOfMonth());
                    endDate = selectedDate.with(TemporalAdjusters.lastDayOfMonth());
                    break;
                case "day":
                default:
                    // Single day
                    startDate = selectedDate;
                    endDate = selectedDate;
                    break;
            }
            
            if (status != null && !status.isEmpty()) {
                reservations = reservationRepository.findByDateRangeAndStatus(startDate, endDate, Reservation.Status.valueOf(status));
            } else {
                reservations = reservationRepository.findByDateRange(startDate, endDate);
            }
        } else if (date != null && !date.isEmpty() && status != null && !status.isEmpty()) {
            // Legacy single date filtering
            reservations = reservationRepository.findByDateAndStatus(LocalDate.parse(date), Reservation.Status.valueOf(status));
        } else if (date != null && !date.isEmpty()) {
            // Legacy single date filtering
            reservations = reservationRepository.findByDate(LocalDate.parse(date));
        } else if (status != null && !status.isEmpty()) {
            reservations = reservationRepository.findByStatus(Reservation.Status.valueOf(status));
        } else {
            reservations = reservationRepository.findAll();
        }

        return reservations;
    }
    @GetMapping("/{id}")
    public Reservation getReservationById(@PathVariable Long id) {
        return reservationRepository.findById(id).orElse(null);
    }

    @PostMapping
    public Reservation createReservation(@RequestBody Reservation newReservation,
                                         @RequestParam(defaultValue = "single") String actionScope) {
        try {
            return reservationService.save(newReservation);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Reservation updateReservation(@PathVariable Long id,
                                         @RequestBody Reservation reservation,
                                         @RequestParam(defaultValue = "single") String actionScope) {
        reservation.setId(id);
        try {
            return reservationService.update(reservation, actionScope);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/{id}/cancel")
    public void cancelReservation(@PathVariable Long id) {
        reservationService.cancel(id);
    }

    @PostMapping("/{id}/cancelSeries")
    public void cancelReservationSeries(@PathVariable Long id) {
        Reservation parentReservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Réservation non trouvée"));
        reservationService.cancelSeries(parentReservation);
    }

    // Fix compilation error by updating ReservationEvent constructor calls
    private void publishCancellationEvent(Reservation reservation) {
        String recurrenceDetails = "";
        eventPublisher.publishEvent(new edbm.salle.demo.events.ReservationEvent(reservation, "annulée", true, false, recurrenceDetails));
    }

    private void publishCancellationEventWithRecurrence(Reservation reservation, String recurrenceDetails) {
        eventPublisher.publishEvent(new edbm.salle.demo.events.ReservationEvent(reservation, "annulée", true, true, recurrenceDetails));
    }

    
    private void cancelSeries(Reservation reservation) {
        if (reservation.getParentReservation() == null) {
            List<Reservation> series = reservationRepository.findByParentReservation(reservation);
            for (Reservation r : series) {
                r.setStatus(Reservation.Status.CANCELLED);
                reservationRepository.save(r);
                // Publish event for each cancelled reservation to trigger email notifications
                eventPublisher.publishEvent(new edbm.salle.demo.events.ReservationEvent(r, "annulée", true, false, ""));
            }
            reservation.setStatus(Reservation.Status.CANCELLED);
            reservationRepository.save(reservation);
            eventPublisher.publishEvent(new edbm.salle.demo.events.ReservationEvent(reservation, "annulée", true, false, ""));
        } else {
            Reservation parent = reservation.getParentReservation();
            cancelSeries(parent);
        }
    }
    
}
