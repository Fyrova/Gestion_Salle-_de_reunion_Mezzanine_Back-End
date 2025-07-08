package edbm.salle.demo.controller;

import edbm.salle.demo.model.Reservation;
import edbm.salle.demo.model.User;
import edbm.salle.demo.repository.ReservationRepository;
import edbm.salle.demo.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReservationController {

    @Autowired
    private ReservationRepository reservationRepository;

    // Remove duplicate declaration of reservationService
    // @Autowired
    // private ReservationService reservationService;

    @GetMapping("/reservations")
    public List<Reservation> getReservationsByFilters(
            @RequestParam(required = false) String organizerName,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status) {

        if (date != null && !date.isEmpty() && status != null && !status.isEmpty()) {
            // Filter by date and status
            return reservationRepository.findByDateAndStatus(java.time.LocalDate.parse(date), Reservation.Status.valueOf(status));
        } else if (date != null && !date.isEmpty()) {
            // Filter by date only
            return reservationRepository.findByDate(java.time.LocalDate.parse(date));
        } else if (status != null && !status.isEmpty()) {
            // Filter by status only
            return reservationRepository.findByStatus(Reservation.Status.valueOf(status));
        } else if (organizerName != null && !organizerName.isEmpty()) {
            return reservationRepository.findByOrganizerNameContainingIgnoreCase(organizerName);
        } else {
            return reservationRepository.findAll();
        }
    }

    @GetMapping("/reservations/{id}")
    public ResponseEntity<Reservation> getReservationById(@PathVariable Long id) {
        Optional<Reservation> reservation = reservationRepository.findById(id);
        if (reservation.isPresent()) {
            return ResponseEntity.ok(reservation.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Autowired
    private ReservationService reservationService;

    @PostMapping("/reservations")
    public ResponseEntity<?> createReservation(@RequestBody Reservation newReservation) {
        try {
            // Basic validation example: check required fields
            if (newReservation.getDate() == null || newReservation.getStartTime() == null || newReservation.getEndTime() == null) {
                return ResponseEntity.badRequest().body("Date, start time and end time are required.");
            }
            // Set default status if not set
            if (newReservation.getStatus() == null) {
                newReservation.setStatus(Reservation.Status.CONFIRMED);
            }
            Reservation savedReservation = reservationService.save(newReservation);
            return ResponseEntity.ok(savedReservation);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating reservation: " + e.getMessage());
        }
    }

    @PutMapping("/reservations/{id}")
    public ResponseEntity<Reservation> updateReservation(@PathVariable Long id, @RequestBody Reservation updatedReservation) {
        Optional<Reservation> existingReservationOpt = reservationRepository.findById(id);
        if (!existingReservationOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Reservation existingReservation = existingReservationOpt.get();
        // Update fields
        existingReservation.setDate(updatedReservation.getDate());
        existingReservation.setStartTime(updatedReservation.getStartTime());
        existingReservation.setEndTime(updatedReservation.getEndTime());
        existingReservation.setSubject(updatedReservation.getSubject());
        // Handle organizer safely by fetching managed entity if id present
        if (updatedReservation.getOrganizer() != null) {
            if (updatedReservation.getOrganizer().getId() != null) {
                // Fetch managed organizer entity
                User managedOrganizer = existingReservation.getOrganizer();
                if (managedOrganizer == null || !managedOrganizer.getId().equals(updatedReservation.getOrganizer().getId())) {
                    // Replace with new managed entity
                    managedOrganizer = updatedReservation.getOrganizer();
                }
         
                existingReservation.setOrganizer(managedOrganizer);
            } else {
                existingReservation.setOrganizer(updatedReservation.getOrganizer());
            }
        }
        // Remove handling of participants as this field was removed
        // Instead, handle participantsCount field
        existingReservation.setParticipantsCount(updatedReservation.getParticipantsCount());
        existingReservation.setReservationType(updatedReservation.getReservationType());
        existingReservation.setEquipment(updatedReservation.getEquipment());
        existingReservation.setDisposition(updatedReservation.getDisposition());
        // Set status directly as Reservation.Status enum
        if (updatedReservation.getStatus() != null) {
            existingReservation.setStatus(updatedReservation.getStatus());
        }
        reservationRepository.save(existingReservation);
        return ResponseEntity.ok(existingReservation);
    }

    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        Optional<Reservation> existingReservationOpt = reservationRepository.findById(id);
        if (!existingReservationOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Reservation reservation = existingReservationOpt.get();
        reservation.setStatus(Reservation.Status.CANCELLED);
        reservationRepository.save(reservation);
        return ResponseEntity.noContent().build();
    }
}
