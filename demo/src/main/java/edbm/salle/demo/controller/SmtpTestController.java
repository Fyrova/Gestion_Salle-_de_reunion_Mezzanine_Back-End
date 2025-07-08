package edbm.salle.demo.controller;

import edbm.salle.demo.service.SmtpTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SmtpTestController {

    @Autowired
    private SmtpTestService smtpTestService;

    @GetMapping("/api/smtp-test")
    public String sendTestEmail(@RequestParam String to) {
        try {
            smtpTestService.sendTestEmail(to);
            return "Email de test envoyé avec succès à " + to;
        } catch (Exception e) {
            return "Erreur lors de l'envoi de l'email de test: " + e.getMessage();
        }
    }
}
