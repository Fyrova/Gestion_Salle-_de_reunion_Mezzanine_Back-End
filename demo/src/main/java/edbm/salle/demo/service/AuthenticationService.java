package edbm.salle.demo.service;

import edbm.salle.demo.model.Role;
import edbm.salle.demo.model.User;
import edbm.salle.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    public User authenticate(String email, String password) throws Exception {
        System.out.println("Authenticating user with email: " + email);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            System.out.println("User not found");
            throw new Exception("Utilisateur non trouvé");
        }

        // Verify password using BCrypt
        if (!BCrypt.checkpw(password, user.getPassword())) {
            System.out.println("Incorrect password");
            throw new Exception("Mot de passe incorrect");
        }

        // Check if user has admin role
        Set<Role> roles = user.getRoles();
        System.out.println("User roles: " + roles);
        boolean isAdmin = roles.stream().anyMatch(role -> "admin".equalsIgnoreCase(role.getRoleName()));
        if (!isAdmin) {
            System.out.println("User is not admin");
            throw new Exception("Accès refusé : utilisateur non administrateur");
        }

        System.out.println("Authentication successful");
        return user;
    }
}
