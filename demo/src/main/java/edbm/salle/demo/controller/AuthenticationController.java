package edbm.salle.demo.controller;

import edbm.salle.demo.model.User;
import edbm.salle.demo.service.AuthenticationService;
import edbm.salle.demo.service.EmailService;
import edbm.salle.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.UUID;


@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    // In-memory token store for simplicity (should be replaced with persistent store)
    private final java.util.Map<String, String> passwordResetTokens = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
        try {
            User user = authenticationService.authenticate(email, password);
            // Return a dummy token for now
            String token = "dummy-auth-token-for-" + user.getEmail();
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Erreur d'authentification : " + e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return "Aucun utilisateur trouvé avec cet email";
        }
        String token = UUID.randomUUID().toString();
        passwordResetTokens.put(token, email);

        String resetLink = "http://localhost:3000/reset-password?token=" + token;
        String subject = "Réinitialisation de votre mot de passe";
        String text = "Bonjour,\n\nPour réinitialiser votre mot de passe, veuillez cliquer sur le lien suivant :\n" + resetLink + "\n\nCordialement,\nEDBM";

        emailService.sendEmailAsync(email, subject, text);
        return "Email de réinitialisation envoyé";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        String email = passwordResetTokens.get(token);
        if (email == null) {
            return "Token invalide ou expiré";
        }
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return "Utilisateur non trouvé";
        }
        // Hash the new password before saving
        String hashedPassword = org.springframework.security.crypto.bcrypt.BCrypt.hashpw(newPassword, org.springframework.security.crypto.bcrypt.BCrypt.gensalt());
        user.setPassword(hashedPassword);
        userRepository.save(user);

        passwordResetTokens.remove(token);
        return "Mot de passe réinitialisé avec succès";
    }
}
