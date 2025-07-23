package edbm.salle.demo.service;

import edbm.salle.demo.events.ReservationEvent;
import edbm.salle.demo.service.ReservationServiceHelper;
import java.time.DayOfWeek;
import edbm.salle.demo.model.Reservation;
import edbm.salle.demo.model.User;
import edbm.salle.demo.repository.ReservationRepository;
import edbm.salle.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReservationService implements IReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    private static final LocalTime WORK_START = LocalTime.of(7, 0);
    private static final LocalTime WORK_END = LocalTime.of(19, 0);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    public List<Reservation> findReservationsForReminder(LocalDateTime from, LocalDateTime to) {
        return reservationRepository.findByStatusAndDateTimeRangeForReminder(
            Reservation.Status.CONFIRMED,
            from.toLocalDate(),
            from.toLocalTime(),
            to.toLocalDate(),
            to.toLocalTime()
        );
    }

    @Transactional
    public Reservation save(Reservation reservation) throws IllegalArgumentException {
        return save(reservation, true, true);
    }

    @Transactional
    public Reservation save(Reservation reservation, boolean publishEvent) throws IllegalArgumentException {
        return save(reservation, publishEvent, true);
    }

    @Transactional
    public Reservation save(Reservation reservation, boolean publishEvent, boolean validateOverlap) throws IllegalArgumentException {
        logger.info("Début de la méthode save dans ReservationService");

        if (reservation.getRecurrenceRule() == null || reservation.getRecurrenceRule().isEmpty()) {
            if (validateOverlap) {
                validateReservation(reservation);
            }

            User organizer = reservation.getOrganizer();
            if (organizer != null) {
                User existingUser = userRepository.findByEmailWithContext(organizer.getEmail());
                if (existingUser != null) {
                    reservation.setOrganizer(existingUser);
                } else {
                    organizer = userRepository.saveAndFlush(organizer);
                    reservation.setOrganizer(organizer);
                }
            }


            if (reservation.getDepartement() == null || reservation.getDepartement().trim().isEmpty()) {
                throw new IllegalArgumentException("Le département est obligatoire");
            }
            Reservation savedReservation = reservationRepository.save(reservation);

                if (publishEvent) {
                logger.info("Publication de l'événement de création de réservation");
                eventPublisher.publishEvent(new ReservationEvent(savedReservation, "créée", true, false, ""));
                logger.info("Événement de création publié avec succès");

                // sendNotificationEmails(savedReservation, "créée", true, false, "");
            }

            logger.info("Fin de la méthode save dans ReservationService");
            return savedReservation;
        } else {
            logger.info("Début de la sauvegarde d'une réservation récurrente");

            // Preprocess recurrenceRule to extract date part from UNTIL field if present
            String recurrenceRule = reservation.getRecurrenceRule();
            if (recurrenceRule != null && recurrenceRule.contains("UNTIL=")) {
                recurrenceRule = recurrenceRule.replaceAll("UNTIL=([^;T]+)T[^;]+", "UNTIL=$1");
            }

            Map<String, String> ruleParts = RRuleHelper.parseRRule(recurrenceRule);
            int count = RRuleHelper.calculateOccurrenceCount(reservation.getDate(), ruleParts);

            if (validateOverlap) {
                validateReservation(reservation);
            }

            User organizer = reservation.getOrganizer();
            if (organizer != null) {
                User existingUser = userRepository.findByEmailWithContext(organizer.getEmail());
                if (existingUser != null) {
                    reservation.setOrganizer(existingUser);
                } else {
                    organizer = userRepository.saveAndFlush(organizer);
                    reservation.setOrganizer(organizer);
                }
            }

            userRepository.flush();
            entityManager.clear();

            if (reservation.getOrganizer() != null) {
                User reattachedOrganizer = entityManager.merge(reservation.getOrganizer());
                reservation.setOrganizer(reattachedOrganizer);
            }

            // Ensure departement is set on parent reservation before saving
            if (reservation.getDepartement() == null) {
                reservation.setDepartement(""); // or set a default value if needed
            }

            // Set endDate from recurrenceRule UNTIL if present
            if (ruleParts.containsKey("UNTIL")) {
                String untilStr = ruleParts.get("UNTIL");
                try {
                    java.time.LocalDate untilDate = java.time.LocalDate.parse(untilStr, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                    reservation.setEndDate(untilDate);
                } catch (Exception e) {
                    logger.warn("Erreur lors de la conversion de la date UNTIL en endDate: " + e.getMessage());
                }
            } else {
                reservation.setEndDate(null);
            }

            String freq = ruleParts.getOrDefault("FREQ", "DAILY");
            int interval = Integer.parseInt(ruleParts.getOrDefault("INTERVAL", "1"));

            // Adjust reservation date to nth weekday if monthly recurrence with BYDAY and BYSETPOS
            if ("MONTHLY".equals(freq) && ruleParts.containsKey("BYDAY") && ruleParts.containsKey("BYSETPOS")) {
                int setPos = Integer.parseInt(ruleParts.get("BYSETPOS"));
                java.time.DayOfWeek dayOfWeek = RRuleHelper.parseDayOfWeek(ruleParts.get("BYDAY"));
                int year = reservation.getDate().getYear();
                int month = reservation.getDate().getMonthValue();
                java.time.LocalDate adjustedDate = ReservationServiceHelper.getNthWeekdayOfMonth(year, month, setPos, dayOfWeek);
                if (adjustedDate != null) {
                    reservation.setDate(adjustedDate);
                }
            }

            Reservation parentReservation = reservationRepository.save(reservation);
            java.util.List<Reservation> occurrencesToSave = new java.util.ArrayList<>();

            if ("MONTHLY".equals(freq) && ruleParts.containsKey("BYDAY") && ruleParts.containsKey("BYSETPOS")) {
                
                occurrencesToSave.addAll(generateMonthlyByDayOccurrences(parentReservation, count, interval, ruleParts, validateOverlap));
            } else {
                
                java.time.LocalDate currentDate = parentReservation.getDate();

if ("WEEKLY".equals(freq) && ruleParts.containsKey("BYDAY")) {
    occurrencesToSave.addAll(generateWeeklyOccurrences(parentReservation, count, interval, ruleParts, validateOverlap));
} else {
    for (int i = 1; i < count; i++) {
        currentDate = RRuleHelper.calculateNextDate(currentDate, freq, interval, ruleParts);
        boolean exists = reservationRepository.existsByParentReservationAndDate(parentReservation, currentDate);
        if (!exists) {
            Reservation occurrence = new Reservation();
            occurrence.setParentReservation(parentReservation);
            occurrence.setDate(currentDate);
            occurrence.setStartTime(parentReservation.getStartTime());
            occurrence.setEndTime(parentReservation.getEndTime());
            occurrence.setSubject(parentReservation.getSubject());
            occurrence.setOrganizer(parentReservation.getOrganizer());
            occurrence.setStatus(parentReservation.getStatus());
            occurrence.setReservationType(parentReservation.getReservationType());
            occurrence.setEquipment(parentReservation.getEquipment());
            occurrence.setDisposition(parentReservation.getDisposition());
            occurrence.setParticipantsCount(parentReservation.getParticipantsCount());
            occurrence.setDepartement(parentReservation.getDepartement());
            occurrence.setRecurrenceRule(null);
            if (validateOverlap) {
                validateReservation(occurrence);
            }
            occurrencesToSave.add(occurrence);
        }
    }
}
            }

            reservationRepository.saveAll(occurrencesToSave);

            if (publishEvent) {
                logger.info("Réservations récurrentes créées avec succès");
                // Only publish event and send email once for the parent reservation
                String recurrenceDetails = buildRecurrenceDatesTable(parentReservation, count, freq, interval, ruleParts);
                eventPublisher.publishEvent(new ReservationEvent(parentReservation, "créée", true, true, recurrenceDetails));
                logger.info("Événement de création publié pour la réservation récurrente");
                //sendNotificationEmails(parentReservation, "créée", true, true, recurrenceDetails);
            }

            return parentReservation;
        }
    }

    private java.util.List<Reservation> generateMonthlyByDayOccurrences(Reservation parentReservation, int count, int interval, Map<String, String> ruleParts, boolean validateOverlap) {
        java.util.List<Reservation> occurrences = new java.util.ArrayList<>();
        java.time.LocalDate currentDate = parentReservation.getDate();

        for (int i = 1; i < count; i++) {
            java.time.LocalDate nextDate = RRuleHelper.calculateNextDate(currentDate, "MONTHLY", interval, ruleParts);
            currentDate = nextDate;
            boolean exists = reservationRepository.existsByParentReservationAndDate(parentReservation, currentDate);
            if (!exists) {
                Reservation occurrence = new Reservation();
                occurrence.setParentReservation(parentReservation);
                occurrence.setDate(currentDate);
                occurrence.setStartTime(parentReservation.getStartTime());
                occurrence.setEndTime(parentReservation.getEndTime());
                occurrence.setSubject(parentReservation.getSubject());
                occurrence.setOrganizer(parentReservation.getOrganizer());
                occurrence.setStatus(parentReservation.getStatus());
                occurrence.setReservationType(parentReservation.getReservationType());
                occurrence.setEquipment(parentReservation.getEquipment());
                occurrence.setDisposition(parentReservation.getDisposition());
                occurrence.setParticipantsCount(parentReservation.getParticipantsCount());
                occurrence.setDepartement(parentReservation.getDepartement());
                occurrence.setRecurrenceRule(null);
                if (validateOverlap) {
                    validateReservation(occurrence);
                }
                occurrences.add(occurrence);
            }
        }
        return occurrences;
    }

private String buildRecurrenceDatesTable(Reservation parentReservation, int count, String freq, int interval, Map<String, String> ruleParts) {
    StringBuilder sb = new StringBuilder();
    sb.append("\n=== Dates des occurrences de la réservation récurrente ===\n");
    sb.append(String.format("%-15s %-10s\n", "Date", "Jour"));

    // Récupérer les occurrences enregistrées en base pour le parentReservation
    List<Reservation> occurrences = reservationRepository.findByParentReservation(parentReservation);
    // Ajouter la date du parent
    sb.append(String.format("%-15s %-10s\n", parentReservation.getDate().toString(), parentReservation.getDate().getDayOfWeek().toString()));

    // Trier les occurrences par date
    occurrences.sort((r1, r2) -> r1.getDate().compareTo(r2.getDate()));

    for (Reservation occurrence : occurrences) {
        sb.append(String.format("%-15s %-10s\n", occurrence.getDate().toString(), occurrence.getDate().getDayOfWeek().toString()));
    }
    return sb.toString();
}

    @Override
    @Transactional
    public Reservation update(Reservation updatedReservation) throws IllegalArgumentException {
        return update(updatedReservation, "single");
    }

    @Transactional
    public Reservation update(Reservation updatedReservation, String actionScope) throws IllegalArgumentException {
        validateReservation(updatedReservation);
        Reservation existingReservation = reservationRepository.findById(updatedReservation.getId())
                .orElseThrow(() -> new IllegalArgumentException("Réservation non trouvée"));

        boolean equipmentRelatedChanged = 
            !existingReservation.getDate().equals(updatedReservation.getDate()) ||
            !existingReservation.getStartTime().equals(updatedReservation.getStartTime()) ||
            !existingReservation.getEndTime().equals(updatedReservation.getEndTime()) ||
            (updatedReservation.getEquipment() != null && !updatedReservation.getEquipment().trim().isEmpty() &&
             !Objects.equals(existingReservation.getEquipment(), updatedReservation.getEquipment()));

        String actionToPublish = "modifiée";
        if (updatedReservation.getStatus() == Reservation.Status.CANCELLED) {
            actionToPublish = "annulée";
        }

        logger.info("Update reservation id: {}, action: {}, equipmentRelatedChanged: {}, equipment: {}", 
            updatedReservation.getId(), actionToPublish, equipmentRelatedChanged, updatedReservation.getEquipment());

        // Handle organizer User entity to avoid detached entity error
        User organizer = updatedReservation.getOrganizer();
        if (organizer != null) {
            User existingUser = userRepository.findByEmailWithContext(organizer.getEmail());
            if (existingUser != null) {
                updatedReservation.setOrganizer(existingUser);
            } else {
                organizer = userRepository.saveAndFlush(organizer);
                updatedReservation.setOrganizer(organizer);
            }
        }

        switch (actionScope.toLowerCase()) {
case "series":
    Reservation parentReservation = existingReservation.getParentReservation() == null ? existingReservation : existingReservation.getParentReservation();
    List<Reservation> series = reservationRepository.findByParentReservation(parentReservation);
    java.time.LocalDate updateFromDate = updatedReservation.getDate();

        // Check if recurrence rule has changed
        boolean recurrenceRuleChanged = !Objects.equals(existingReservation.getRecurrenceRule(), updatedReservation.getRecurrenceRule());

        // Parse old and new recurrence rules to compare components
        Map<String, String> oldRuleParts = RRuleHelper.parseRRule(existingReservation.getRecurrenceRule());
        Map<String, String> newRuleParts = RRuleHelper.parseRRule(updatedReservation.getRecurrenceRule());

        // Compare relevant components to detect changes
        boolean recurrenceComponentsChanged = !Objects.equals(oldRuleParts.get("FREQ"), newRuleParts.get("FREQ")) ||
                                              !Objects.equals(oldRuleParts.get("INTERVAL"), newRuleParts.get("INTERVAL")) ||
                                              !Objects.equals(oldRuleParts.get("BYDAY"), newRuleParts.get("BYDAY")) ||
                                              !Objects.equals(oldRuleParts.get("UNTIL"), newRuleParts.get("UNTIL")) ||
                                              !Objects.equals(oldRuleParts.get("COUNT"), newRuleParts.get("COUNT")) ||
                                              !Objects.equals(oldRuleParts.get("BYSETPOS"), newRuleParts.get("BYSETPOS"));

        if (recurrenceRuleChanged || recurrenceComponentsChanged) {
            // Delete old occurrences except parent
            for (Reservation r : series) {
                if (!r.equals(parentReservation)) {
                    reservationRepository.delete(r);
                }
            }
            reservationRepository.flush();
        // Regenerate occurrences based on new recurrence rule
        Map<String, String> ruleParts = RRuleHelper.parseRRule(updatedReservation.getRecurrenceRule());
        int count = RRuleHelper.calculateOccurrenceCount(updatedReservation.getDate(), ruleParts);
        int interval = Integer.parseInt(ruleParts.getOrDefault("INTERVAL", "1"));
        String freq = ruleParts.getOrDefault("FREQ", "DAILY");

        java.util.List<Reservation> newOccurrences = new java.util.ArrayList<>();

        if ("MONTHLY".equals(freq) && ruleParts.containsKey("BYDAY") && ruleParts.containsKey("BYSETPOS")) {
            newOccurrences.addAll(generateMonthlyByDayOccurrences(updatedReservation, count, interval, ruleParts, true));
        } else if ("WEEKLY".equals(freq) && ruleParts.containsKey("BYDAY")) {
            newOccurrences.addAll(generateWeeklyOccurrences(updatedReservation, count, interval, ruleParts, true));
        } else {
            java.time.LocalDate currentDate = updatedReservation.getDate();
            for (int i = 1; i < count; i++) {
                currentDate = RRuleHelper.calculateNextDate(currentDate, freq, interval, ruleParts);
                boolean exists = reservationRepository.existsByParentReservationAndDate(updatedReservation, currentDate);
                if (!exists) {
                    Reservation occurrence = new Reservation();
                    occurrence.setParentReservation(updatedReservation);
                    occurrence.setDate(currentDate);
                    occurrence.setStartTime(updatedReservation.getStartTime());
                    occurrence.setEndTime(updatedReservation.getEndTime());
                    occurrence.setSubject(updatedReservation.getSubject());
                    occurrence.setOrganizer(updatedReservation.getOrganizer());
                    occurrence.setStatus(updatedReservation.getStatus());
                    occurrence.setReservationType(updatedReservation.getReservationType());
                    occurrence.setEquipment(updatedReservation.getEquipment());
                    occurrence.setDisposition(updatedReservation.getDisposition());
                    occurrence.setParticipantsCount(updatedReservation.getParticipantsCount());
                    occurrence.setDepartement(updatedReservation.getDepartement());
                    occurrence.setRecurrenceRule(null);
                    validateReservation(occurrence);
                    newOccurrences.add(occurrence);
                }
            }
        }

        reservationRepository.saveAll(newOccurrences);

        logger.info("Publication de l'événement de modification de la série de réservations avec changement de règle de récurrence");
        String recurrenceDetails = buildRecurrenceDatesTable(updatedReservation, count, freq, interval, ruleParts);
                eventPublisher.publishEvent(new ReservationEvent(updatedReservation, actionToPublish, equipmentRelatedChanged, true, recurrenceDetails));
                //sendNotificationEmails(updatedReservation, actionToPublish, true, true, recurrenceDetails);
                // Ajout de l'envoi de notification uniquement pour le matériel
                if (updatedReservation.getEquipment() != null && !updatedReservation.getEquipment().trim().isEmpty()) {
                    String details = buildReservationDetails(updatedReservation);
                    emailService.sendEquipmentNotification(actionToPublish, details, true, recurrenceDetails);
                    System.out.println("Email de notification envoyé au responsable matériel");
                }
                logger.info("Événement de modification publié avec succès");

        return updatedReservation;
        } else {
            // Update occurrences in memory without changing recurrence rule
            for (Reservation r : series) {
                if (!r.getDate().isBefore(updateFromDate)) {
                    // Calculate the difference in days from the original date to the updated date
                    long daysDifference = r.getDate().toEpochDay() - parentReservation.getDate().toEpochDay();
                    r.setDate(updatedReservation.getDate().plusDays(daysDifference));
                    r.setStartTime(updatedReservation.getStartTime());
                    r.setEndTime(updatedReservation.getEndTime());
                    r.setSubject(updatedReservation.getSubject());
                    r.setStatus(updatedReservation.getStatus());
                    r.setReservationType(updatedReservation.getReservationType());
                    r.setEquipment(updatedReservation.getEquipment());
                    r.setDisposition(updatedReservation.getDisposition());
                    r.setParticipantsCount(updatedReservation.getParticipantsCount());
                    validateReservation(r);
                }
            }

            // Batch save all updated occurrences at once
            reservationRepository.saveAll(series);

            if (!parentReservation.getDate().isBefore(updateFromDate)) {
                parentReservation.setDate(updatedReservation.getDate());
                parentReservation.setStartTime(updatedReservation.getStartTime());
                parentReservation.setEndTime(updatedReservation.getEndTime());
                parentReservation.setSubject(updatedReservation.getSubject());
                parentReservation.setStatus(updatedReservation.getStatus());
                parentReservation.setReservationType(updatedReservation.getReservationType());
                parentReservation.setEquipment(updatedReservation.getEquipment());
                parentReservation.setDisposition(updatedReservation.getDisposition());
                parentReservation.setParticipantsCount(updatedReservation.getParticipantsCount());
                validateReservation(parentReservation);
                Reservation savedParent = reservationRepository.save(parentReservation);

                logger.info("Publication de l'événement de modification de la série de réservations");
                Map<String, String> ruleParts = RRuleHelper.parseRRule(savedParent.getRecurrenceRule());
                int count = RRuleHelper.calculateOccurrenceCount(savedParent.getDate(), ruleParts);
                String freq = ruleParts.getOrDefault("FREQ", "DAILY");
                int interval = Integer.parseInt(ruleParts.getOrDefault("INTERVAL", "1"));
                String recurrenceDetails = buildRecurrenceDatesTable(savedParent, count, freq, interval, ruleParts);
                eventPublisher.publishEvent(new ReservationEvent(savedParent, actionToPublish, equipmentRelatedChanged, true, recurrenceDetails));
                logger.info("Événement de modification publié avec succès");

                //sendNotificationEmails(savedParent, actionToPublish, equipmentRelatedChanged, false, "");

                return savedParent;
            }
        }
    break;

            case "partial":
                existingReservation.setDate(updatedReservation.getDate());
                existingReservation.setStartTime(updatedReservation.getStartTime());
                existingReservation.setEndTime(updatedReservation.getEndTime());
                existingReservation.setSubject(updatedReservation.getSubject());
                existingReservation.setStatus(updatedReservation.getStatus());
                existingReservation.setReservationType(updatedReservation.getReservationType());
                existingReservation.setEquipment(updatedReservation.getEquipment());
                existingReservation.setDisposition(updatedReservation.getDisposition());
                existingReservation.setParticipantsCount(updatedReservation.getParticipantsCount());

                Reservation savedPartial = reservationRepository.save(existingReservation);

                logger.info("Publication de l'événement de modification partielle de réservation");
                eventPublisher.publishEvent(new ReservationEvent(savedPartial, actionToPublish, equipmentRelatedChanged, false, ""));
                logger.info("Événement de modification partielle publié avec succès");

                //sendNotificationEmails(savedPartial, actionToPublish, equipmentRelatedChanged, false, "");


                return savedPartial;

            case "single":
            default:
                existingReservation.setDate(updatedReservation.getDate());
                existingReservation.setStartTime(updatedReservation.getStartTime());
                existingReservation.setEndTime(updatedReservation.getEndTime());
                existingReservation.setSubject(updatedReservation.getSubject());
                existingReservation.setStatus(updatedReservation.getStatus());
                existingReservation.setReservationType(updatedReservation.getReservationType());
                existingReservation.setEquipment(updatedReservation.getEquipment());
                existingReservation.setDisposition(updatedReservation.getDisposition());
                existingReservation.setParticipantsCount(updatedReservation.getParticipantsCount());

                Reservation savedReservation = reservationRepository.save(existingReservation);

                logger.info("Publication de l'événement de modification de réservation");
                eventPublisher.publishEvent(new ReservationEvent(savedReservation, actionToPublish, equipmentRelatedChanged, false, ""));
                logger.info("Événement de modification publié avec succès");

                //sendNotificationEmails(savedReservation, actionToPublish, equipmentRelatedChanged, false, "");

                return savedReservation;
        }
        return existingReservation;
    }

    @Transactional
    public void cancel(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Réservation non trouvée"));
        reservation.setStatus(Reservation.Status.CANCELLED);
        Reservation savedReservation = reservationRepository.save(reservation);

        logger.info("Publication de l'événement d'annulation de réservation");
        String recurrenceDetails = ""; // Add recurrence details if applicable
        eventPublisher.publishEvent(new ReservationEvent(savedReservation, "annulée", true, false, recurrenceDetails));
        logger.info("Événement d'annulation publié avec succès");
    }

    @Transactional
    public void cancelSeries(Reservation parentReservation) {
        if (parentReservation == null) {
            throw new IllegalArgumentException("La réservation parente est nulle");
        }
        Map<String, String> ruleParts = RRuleHelper.parseRRule(parentReservation.getRecurrenceRule());
        int count = RRuleHelper.calculateOccurrenceCount(parentReservation.getDate(), ruleParts);
        String freq = ruleParts.getOrDefault("FREQ", "DAILY");
        int interval = Integer.parseInt(ruleParts.getOrDefault("INTERVAL", "1"));
        String recurrenceDetails = buildRecurrenceDatesTable(parentReservation, count, freq, interval, ruleParts);

        List<Reservation> series = reservationRepository.findByParentReservation(parentReservation);
        for (Reservation r : series) {
            r.setStatus(Reservation.Status.CANCELLED);
            reservationRepository.save(r);
            logger.info("Annulation de la réservation id: {}", r.getId());
        }
        parentReservation.setStatus(Reservation.Status.CANCELLED);
        reservationRepository.save(parentReservation);
        logger.info("Publication de l'événement d'annulation pour la réservation parente id: {}", parentReservation.getId());
        // Publish only one event for the parent reservation to send a single email
        eventPublisher.publishEvent(new ReservationEvent(parentReservation, "annulée", true, true, recurrenceDetails));
    }

    @Transactional
    public void delete(Long id) {
        reservationRepository.deleteById(id);
    }

    private void sendNotificationEmails(Reservation reservation, String action, boolean equipmentAffected, boolean isRecurring, String recurrenceDetails) {
        if (reservation.getOrganizer() != null && reservation.getOrganizer().getEmail() != null) {
            String details = buildReservationDetails(reservation);
            emailService.sendReservationConfirmation(
                reservation.getOrganizer().getEmail(),
                reservation.getOrganizer().getName(),
                action,
                details,
                isRecurring,
                recurrenceDetails
            );
            System.out.println("Email de confirmation envoyé à l'organisateur");
        }

        boolean shouldNotifyEquipment = (equipmentAffected || action.equals("créée") || action.equals("annulée") || action.equals("modifiée")) 
                && reservation.getEquipment() != null && !reservation.getEquipment().trim().isEmpty();
        
        if (shouldNotifyEquipment) {
            String details = buildReservationDetails(reservation);
            emailService.sendEquipmentNotification(action, details, isRecurring, recurrenceDetails);
            System.out.println("Email de notification envoyé au responsable matériel");
        }
    }

    private String buildReservationDetails(Reservation reservation) {
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

    private void validateReservation(Reservation reservation) throws IllegalArgumentException {
        validateReservation(reservation, null);
    }

    private void validateReservation(Reservation reservation, Reservation excludeReservation) throws IllegalArgumentException {
        if (reservation.getStartTime().isBefore(WORK_START) || reservation.getEndTime().isAfter(WORK_END)) {
            throw new IllegalArgumentException("La réservation doit être entre 7h et 19h");
        }
        if (!reservation.getStartTime().isBefore(reservation.getEndTime())) {
            throw new IllegalArgumentException("L'heure de début doit être avant l'heure de fin");
        }
        
        List<Reservation> overlapping = reservationRepository.findAll().stream()
                .filter(r -> r.getDate().equals(reservation.getDate()))
                .filter(r -> r.getStatus() == Reservation.Status.CONFIRMED)
                .filter(r -> !r.getId().equals(reservation.getId()))
                .filter(r -> excludeReservation == null || !r.getId().equals(excludeReservation.getId()))
                .filter(r -> !(reservation.getEndTime().isBefore(r.getStartTime()) || reservation.getEndTime().equals(r.getStartTime()) ||
                               reservation.getStartTime().isAfter(r.getEndTime()) || reservation.getStartTime().equals(r.getEndTime())))
                .toList();
                
        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("Créneau déjà réservé");
        }
    }

    private List<Reservation> generateWeeklyOccurrences(Reservation parentReservation, int count, 
                                                  int interval, Map<String, String> ruleParts, 
                                                  boolean validateOverlap) {
    List<Reservation> occurrences = new ArrayList<>();
    LocalDate startDate = parentReservation.getDate();
    
    // Parse and sort days of week
    String byDay = ruleParts.get("BYDAY");
    List<DayOfWeek> selectedDays = Arrays.stream(byDay.split(","))
                                      .map(RRuleHelper::parseDayOfWeek)
                                      .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                                      .collect(Collectors.toList());

    // Calculate until date if exists
    LocalDate untilDate = ruleParts.containsKey("UNTIL") 
        ? LocalDate.parse(ruleParts.get("UNTIL").split("T")[0], DateTimeFormatter.BASIC_ISO_DATE)
        : null;

    int occurrencesGenerated = 0;
    int weeksToAdd = 0;

    while (occurrencesGenerated < count - 1) {
        for (DayOfWeek day : selectedDays) {
            if (occurrencesGenerated >= count - 1) break;

            LocalDate occurrenceDate = startDate.plusWeeks(weeksToAdd).with(day);
            
            // Skip dates before start date (for first week)
            if (occurrenceDate.isBefore(startDate)) continue;
            
            // Skip if beyond until date
            if (untilDate != null && occurrenceDate.isAfter(untilDate)) continue;
            
            // Skip parent date
            if (occurrenceDate.equals(startDate)) continue;

            // Check for existing occurrences
            if (!reservationRepository.existsByParentReservationAndDate(parentReservation, occurrenceDate)) {
                Reservation occurrence = createOccurrenceFromParent(parentReservation);
                occurrence.setDate(occurrenceDate);
                
                if (validateOverlap) {
                    validateReservation(occurrence, parentReservation);
                }
                
                occurrences.add(occurrence);
                occurrencesGenerated++;
            }
        }
        weeksToAdd += interval;
        
        // Additional check for untilDate to prevent infinite loop
        if (untilDate != null && startDate.plusWeeks(weeksToAdd).isAfter(untilDate)) {
            break;
        }
    }

    return occurrences;
    }
    private List<DayOfWeek> parseByDayString(String byDay) {
        String[] days = byDay.split(",");
        List<DayOfWeek> daysOfWeek = new ArrayList<>();
        
        for (String day : days) {
            daysOfWeek.add(RRuleHelper.parseDayOfWeek(day));
        }
        
        return daysOfWeek;
    }

    private Reservation createOccurrenceFromParent(Reservation parentReservation) {
        Reservation occurrence = new Reservation();
        occurrence.setParentReservation(parentReservation);
        occurrence.setStartTime(parentReservation.getStartTime());
        occurrence.setEndTime(parentReservation.getEndTime());
        occurrence.setSubject(parentReservation.getSubject());
        occurrence.setOrganizer(parentReservation.getOrganizer());
        occurrence.setStatus(parentReservation.getStatus());
        occurrence.setReservationType(parentReservation.getReservationType());
        occurrence.setEquipment(parentReservation.getEquipment());
        occurrence.setDisposition(parentReservation.getDisposition());
        occurrence.setParticipantsCount(parentReservation.getParticipantsCount());
        occurrence.setDepartement(parentReservation.getDepartement());
        occurrence.setRecurrenceRule(null);
        return occurrence;
    }
}
