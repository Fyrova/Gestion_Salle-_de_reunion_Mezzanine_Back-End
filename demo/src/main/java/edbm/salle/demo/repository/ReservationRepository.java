package edbm.salle.demo.repository;

import edbm.salle.demo.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.date = :date")
    long countByDate(@Param("date") LocalDate date);

    // Remove this query as participants field no longer exists
    // Replace with a method to sum participantsCount for a given date
    @Query("SELECT SUM(COALESCE(r.participantsCount, 0)) FROM Reservation r WHERE r.date = :date")
    Long countParticipantsByDate(@Param("date") LocalDate date);

    @Query("SELECT r FROM Reservation r WHERE r.date = :date ORDER BY r.startTime")
    List<Reservation> findReservationsByDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(FUNCTION('TIMESTAMPDIFF', MINUTE, r.startTime, r.endTime)) FROM Reservation r WHERE r.date = :date")
    Long sumReservedMinutesByDate(@Param("date") LocalDate date);

    @Query("SELECT r FROM Reservation r WHERE r.createdAt >= :startOfDay AND r.createdAt < :endOfDay ORDER BY r.startTime")
    List<Reservation> findReservationsCreatedOnDate(@Param("startOfDay") java.time.LocalDateTime startOfDay, @Param("endOfDay") java.time.LocalDateTime endOfDay);

    @Query("SELECT r FROM Reservation r WHERE LOWER(r.organizer.name) LIKE LOWER(CONCAT('%', :organizerName, '%')) ORDER BY r.date, r.startTime")
    List<Reservation> findByOrganizerNameContainingIgnoreCase(@Param("organizerName") String organizerName);

    List<Reservation> findByDateAndStatus(LocalDate date, Reservation.Status status);

    List<Reservation> findByDate(LocalDate date);

    List<Reservation> findByStatus(Reservation.Status status);
}
