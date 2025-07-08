package edbm.salle.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.equipment.manager.email}")
    private String equipmentManagerEmail;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Async
    public void sendEmail(String to, String subject, String text) {
        System.out.println("Envoi d'email à : " + to + " avec le sujet : " + subject);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
        System.out.println("Email envoyé avec succès à : " + to);
    }

    @Async
    public void sendReservationConfirmation(String to, String organizerName, String action, String reservationDetails) {
        System.out.println("Préparation de l'email de confirmation pour " + to);
        String subject = "Confirmation de votre réservation - " + action;
        String text = "Bonjour " + organizerName + ",\n\n" +
                "Votre réservation a été " + action.toLowerCase() + " avec succès.\n\n" +
                "Détails de la réservation:\n" + reservationDetails + "\n\n" +
                "Merci,\nEDBM";
        sendEmail(to, subject, text);
        System.out.println("Email de confirmation envoyé à " + to);
    }

    @Async
    public void sendEquipmentNotification(String action, String reservationDetails) {
        System.out.println("Préparation de l'email de notification pour le responsable du matériel: " + equipmentManagerEmail);
        String subject = "Notification matériel pour une réunion - " + action;
        String text = "Bonjour,\n\n" +
                "Veuillez noter la réunion suivante nécessitant du matériel:\n\n" +
                reservationDetails + "\n\n" +
                "Merci de préparer le matériel nécessaire avant la réunion.\n\n" +
                "Cordialement,\nEDBM";
        sendEmail(equipmentManagerEmail, subject, text);
        System.out.println("Email de notification envoyé au responsable du matériel");
    }
}
