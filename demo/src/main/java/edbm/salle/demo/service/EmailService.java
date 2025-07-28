package edbm.salle.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import edbm.salle.demo.model.Reservation;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.equipment.manager.email}")
    private String equipmentManagerEmail;

    @Value("${spring.mail.username}")
    private String mailFrom;

    public void sendEmailSync(String to, String subject, String text) {
        try {
            logger.info("Tentative d'envoi EMAIL à: {}", to);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            logger.info("EMAIL envoyé avec succès à: {}", to);
        } catch (Exception e) {
            logger.error("Échec d'envoi EMAIL à {}", to, e);
            throw new RuntimeException("Échec d'envoi email", e);
        }
    }

    public String buildReservationDetails(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Date: ").append(reservation.getDate()).append("\n");
        sb.append("Heure: ").append(reservation.getStartTime()).append(" - ").append(reservation.getEndTime()).append("\n");
        sb.append("Objet: ").append(reservation.getSubject()).append("\n");
        sb.append("Type: ").append(reservation.getReservationType()).append("\n");
        sb.append("Disposition: ").append(reservation.getDisposition()).append("\n");
        sb.append("Participants: ").append(reservation.getParticipantsCount()).append("\n");
        if (reservation.getEquipment() != null) {
            sb.append("Équipement: ").append(reservation.getEquipment()).append("\n");
        }
        return sb.toString();
    }

    @Async
    public void sendEmailAsync(String to, String subject, String text) {
        sendEmailSync(to, subject, text);
    }

    public void sendReservationConfirmation(String to, String organizerName, String action, String details, boolean isRecurring, String recurrenceDetails) {
        String subject = "Confirmation de réservation - " + action;
        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append(String.format(
            "Bonjour %s,\n\nVotre réservation a été %s.\n\n", organizerName, action.toLowerCase()));
        if (isRecurring) {
            textBuilder.append("Il s'agit d'une réservation récurrente avec les dates suivantes:\n");
            textBuilder.append(recurrenceDetails).append("\n\n");
        }
        textBuilder.append("Détails:\n").append(details).append("\n\nCordialement,\nEDBM");
        sendEmailSync(to, subject, textBuilder.toString());
    }

    public void sendEquipmentNotification(String action, String details, boolean isRecurring, String recurrenceDetails) {
        String subject = "Notification matériel - " + action;
        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append(String.format(
            "Bonjour,\n\nNouvelle notification pour réservation %s:\n\n", action.toLowerCase()));
        if (isRecurring) {
            textBuilder.append("Il s'agit d'une réservation récurrente avec les détails suivants:\n");
            textBuilder.append(recurrenceDetails).append("\n\n");
        }
        textBuilder.append(details).append("\n\nMerci de préparer le matériel.\n\nCordialement,\nEDBM");
        sendEmailSync(equipmentManagerEmail, subject, textBuilder.toString());
    }

    public void sendReminderEmail(String to, String organizerName, Reservation reservation) {
        String subject = "Rappel: Réunion dans quelques heures - " + reservation.getSubject();
        String text = String.format(
            "Bonjour %s,\n\nCeci est un rappel que votre réunion confirmée est prévue dans quelques heures.\n\nDétails:\nDate: %s\nHeure: %s - %s\nObjet: %s\n\nCordialement,\nEDBM",
            organizerName,
            reservation.getDate(),
            reservation.getStartTime(),
            reservation.getEndTime(),
            reservation.getSubject()
        );
        sendEmailSync(to, subject, text);
    }
}

