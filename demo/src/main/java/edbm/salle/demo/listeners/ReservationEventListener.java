package edbm.salle.demo.listeners;

import edbm.salle.demo.events.ReservationEvent;
import edbm.salle.demo.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservationEventListener {
    private static final Logger logger = LoggerFactory.getLogger(ReservationEventListener.class);

    @Autowired
    private EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationEvent(ReservationEvent event) {
        logger.info("Début du traitement de l'événement pour action: {}", event.getAction());
        logger.info("equipmentAffected flag: {}", event.isEquipmentAffected());
        try {
            sendNotificationEmails(event);
            logger.info("Fin du traitement de l'événement pour action: {}", event.getAction());
        } catch (Exception e) {
            logger.error("Échec du traitement de l'événement", e);
        }
    }

    private void sendNotificationEmails(ReservationEvent event) {
        var reservation = event.getReservation();
        var action = event.getAction();
        var details = emailService.buildReservationDetails(reservation);

        if (reservation.getOrganizer() != null && reservation.getOrganizer().getEmail() != null) {
            logger.info("Envoi email de confirmation à {}", reservation.getOrganizer().getEmail());
            emailService.sendReservationConfirmation(
                reservation.getOrganizer().getEmail(),
                reservation.getOrganizer().getName(),
                action,
                details,
                event.isRecurring(),
                event.getRecurrenceDetails()
            );
            logger.info("Email de confirmation envoyé à {}", reservation.getOrganizer().getEmail());
        } else {
            logger.warn("Pas d'organisateur ou d'email pour la réservation");
        }

        if (event.isEquipmentAffected() && reservation.getEquipment() != null && !reservation.getEquipment().trim().isEmpty()) {
            logger.info("Envoi email de notification au responsable matériel, équipement: '{}'", reservation.getEquipment());
            emailService.sendEquipmentNotification(action, details, event.isRecurring(), event.getRecurrenceDetails());
        } else {
            logger.warn("Pas d'équipement affecté ou équipement vide, équipement: '{}'", reservation.getEquipment());
        }
    }

    private String buildDetails(edbm.salle.demo.model.Reservation reservation) {
        return emailService.buildReservationDetails(reservation);
    }
}
