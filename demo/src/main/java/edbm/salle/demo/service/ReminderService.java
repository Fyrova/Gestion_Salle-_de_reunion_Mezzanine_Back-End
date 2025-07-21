package edbm.salle.demo.service;

import edbm.salle.demo.model.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReminderService {
    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private EmailService emailService;

    @Scheduled(fixedRate = 300000)
    public void sendReminders() {
        logger.info("Début de la tâche planifiée d'envoi des rappels");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);

        List<Reservation> upcomingReservations = reservationService.findReservationsForReminder(now, oneHourLater);

        for (Reservation reservation : upcomingReservations) {
            try {
                logger.info("Envoi du rappel pour la réservation id: {}", reservation.getId());
                emailService.sendReminderEmail(
                    reservation.getOrganizer().getEmail(),
                    reservation.getOrganizer().getName(),
                    reservation
                );
                reservation.setReminderSentAt(LocalDateTime.now());
                reservationService.save(reservation, false, false); // Save updated reminderSentAt without publishing event and without overlap validation
            } catch (Exception e) {
                logger.error("Erreur lors de l'envoi du rappel pour la réservation id: " + reservation.getId(), e);
            }
        }

        logger.info("Fin de la tâche planifiée d'envoi des rappels");
    }
}
