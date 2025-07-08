package edbm.salle.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpTestService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendTestEmail(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@edbm.mg");
        message.setTo(to);
        message.setSubject("Test de connexion SMTP");
        message.setText("Ceci est un email de test pour vérifier la configuration SMTP.");
        mailSender.send(message);
    }
}
