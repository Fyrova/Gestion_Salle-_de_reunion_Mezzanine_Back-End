package edbm.salle.demo.service;

import edbm.salle.demo.model.Reservation;
import edbm.salle.demo.model.User;
import edbm.salle.demo.repository.ReservationRepository;
import edbm.salle.demo.repository.UserRepository;
import edbm.salle.demo.repository.ParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservationService {

    private static final LocalTime WORK_START = LocalTime.of(7, 0);
    private static final LocalTime WORK_END = LocalTime.of(19, 0);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private EmailService emailService;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    @Transactional
    public Reservation save(Reservation reservation) throws IllegalArgumentException {
        System.out.println("Début de la méthode save dans ReservationService");
        validateReservation(reservation);
        // Save organizer if new or update existing
        User organizer = reservation.getOrganizer();
        if (organizer != null) {
            System.out.println("Organisateur email: " + organizer.getEmail());
            // Check if email already exists to avoid duplicate key error
            User existingUser = userRepository.findByEmailWithContext(organizer.getEmail());
            if (existingUser != null) {
                System.out.println("Utilisateur existant trouvé avec email: " + existingUser.getEmail());
                // Replace organizer with managed entity to avoid duplicate insert
                reservation.setOrganizer(existingUser);
            } else {
                System.out.println("Aucun utilisateur existant trouvé, création d'un nouvel utilisateur");
                // Save new organizer and flush to ensure ID is generated before saving reservation
                organizer = userRepository.saveAndFlush(organizer);
                reservation.setOrganizer(organizer);
            }
        } else {
            System.out.println("Aucun organisateur fourni dans la réservation");
        }
        // Flush to ensure organizer is saved before reservation
        userRepository.flush();
        // Clear persistence context to avoid unintended inserts
        // Use EntityManager injected to clear persistence context
        entityManager.clear();
        // Reattach the organizer entity to the persistence context
        User reattachedOrganizer = entityManager.merge(reservation.getOrganizer());
        reservation.setOrganizer(reattachedOrganizer);
        // Save reservation first to generate ID
        Reservation savedReservation = reservationRepository.save(reservation);

        // Send notification emails after creation
        sendNotificationEmails(savedReservation, "créée");

        // Log message about email sending
        System.out.println("Email(s) envoyé(s) à l'organisateur et/ou au responsable du matériel.");

        System.out.println("Fin de la méthode save dans ReservationService");
        return savedReservation;
    }

    @Transactional
    public Reservation update(Reservation updatedReservation) throws IllegalArgumentException {
        validateReservation(updatedReservation);
        Reservation existingReservation = reservationRepository.findById(updatedReservation.getId())
                .orElseThrow(() -> new IllegalArgumentException("Réservation non trouvée"));

        boolean notifyEquipmentManager = false;

        // Check if date, time or equipment changed
        if (!existingReservation.getDate().equals(updatedReservation.getDate()) ||
            !existingReservation.getStartTime().equals(updatedReservation.getStartTime()) ||
            !existingReservation.getEndTime().equals(updatedReservation.getEndTime()) ||
            (existingReservation.getEquipment() == null && updatedReservation.getEquipment() != null) ||
            (existingReservation.getEquipment() != null && !existingReservation.getEquipment().equals(updatedReservation.getEquipment()))
        ) {
            notifyEquipmentManager = true;
        }

        existingReservation.setDate(updatedReservation.getDate());
        existingReservation.setStartTime(updatedReservation.getStartTime());
        existingReservation.setEndTime(updatedReservation.getEndTime());
        existingReservation.setSubject(updatedReservation.getSubject());
        existingReservation.setOrganizer(updatedReservation.getOrganizer());
        existingReservation.setStatus(updatedReservation.getStatus());
        existingReservation.setReservationType(updatedReservation.getReservationType());
        existingReservation.setEquipment(updatedReservation.getEquipment());
        existingReservation.setDisposition(updatedReservation.getDisposition());
        existingReservation.setParticipantsCount(updatedReservation.getParticipantsCount());

        Reservation savedReservation = reservationRepository.save(existingReservation);

        // Send notification emails after update
        sendNotificationEmails(savedReservation, "modifiée", notifyEquipmentManager);

        return savedReservation;
    }

    @Transactional
    public void cancel(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Réservation non trouvée"));
        reservation.setStatus(Reservation.Status.CANCELLED);
        reservationRepository.save(reservation);

        // Send notification emails after cancellation
        sendNotificationEmails(reservation, "annulée");
    }

    private void sendNotificationEmails(Reservation reservation, String action) {
        if (reservation.getOrganizer() != null && reservation.getOrganizer().getEmail() != null) {
            String reservationDetails = buildReservationDetails(reservation);
            emailService.sendReservationConfirmation(reservation.getOrganizer().getEmail(),
                    reservation.getOrganizer().getName(), action, reservationDetails);
        }
        if (reservation.getEquipment() != null && !reservation.getEquipment().isEmpty()) {
            String reservationDetails = buildReservationDetails(reservation);
            emailService.sendEquipmentNotification(action, reservationDetails);
        }
    }

    private void sendNotificationEmails(Reservation reservation, String action, boolean notifyEquipmentManager) {
        boolean organizerNotified = false;
        boolean equipmentManagerNotified = false;

        if (reservation.getOrganizer() != null && reservation.getOrganizer().getEmail() != null) {
            String reservationDetails = buildReservationDetails(reservation);
            emailService.sendReservationConfirmation(reservation.getOrganizer().getEmail(),
                    reservation.getOrganizer().getName(), action, reservationDetails);
            organizerNotified = true;
        }
        if (notifyEquipmentManager && reservation.getEquipment() != null && !reservation.getEquipment().isEmpty()) {
            String reservationDetails = buildReservationDetails(reservation);
            emailService.sendEquipmentNotification(action, reservationDetails);
            equipmentManagerNotified = true;
        }

        // Log message about email sending status
        if (organizerNotified) {
            System.out.println("Email de confirmation envoyé à l'organisateur.");
        }
        if (equipmentManagerNotified) {
            System.out.println("Email de notification envoyé au responsable du matériel.");
        }
        if (!organizerNotified && !equipmentManagerNotified) {
            System.out.println("Aucun email envoyé, aucun destinataire trouvé.");
        }
    }

    private String buildReservationDetails(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Date: ").append(reservation.getDate()).append("\n");
        sb.append("Heure: ").append(reservation.getStartTime()).append(" - ").append(reservation.getEndTime()).append("\n");
        sb.append("Objet: ").append(reservation.getSubject()).append("\n");
        sb.append("Type de réservation: ").append(reservation.getReservationType()).append("\n");
        sb.append("Disposition: ").append(reservation.getDisposition()).append("\n");
        sb.append("Nombre de participants: ").append(reservation.getParticipantsCount()).append("\n");
        sb.append("Équipement: ").append(reservation.getEquipment()).append("\n");
        return sb.toString();
    }

    private void validateReservation(Reservation reservation) {
        if (reservation.getStartTime().isBefore(WORK_START) || reservation.getEndTime().isAfter(WORK_END)) {
            throw new IllegalArgumentException("La réservation doit être dans les heures ouvrables (7h00 - 19h00).");
        }
        if (!reservation.getStartTime().isBefore(reservation.getEndTime())) {
            throw new IllegalArgumentException("L'heure de début doit être avant l'heure de fin.");
        }
        // Check for overlapping reservations
        List<Reservation> overlapping = reservationRepository.findAll().stream()
                .filter(r -> r.getDate().equals(reservation.getDate()))
                .filter(r -> r.getStatus() == Reservation.Status.CONFIRMED)
                .filter(r -> !(reservation.getEndTime().isBefore(r.getStartTime()) || reservation.getStartTime().isAfter(r.getEndTime())))
                .toList();
        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("Le créneau horaire est déjà réservé.");
        }
    }

    @Transactional
    public void delete(Long id) {
        reservationRepository.deleteById(id);
    }
}
