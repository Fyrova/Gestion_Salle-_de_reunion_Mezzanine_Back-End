package edbm.salle.demo.repository;

import edbm.salle.demo.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByParentReservationAndDate(Reservation parentReservation, LocalDate date);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.date = :date")
    long countByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.date = :date AND r.status = :status")
    long countByDateAndStatus(@Param("date") LocalDate date, @Param("status") Reservation.Status status);

    @Query("SELECT SUM(COALESCE(r.participantsCount, 0)) FROM Reservation r WHERE r.date = :date")
    Long countParticipantsByDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(COALESCE(r.participantsCount, 0)) FROM Reservation r WHERE r.date = :date AND r.status = :status")
    Long countParticipantsByDateAndStatus(@Param("date") LocalDate date, @Param("status") Reservation.Status status);

    @Query("SELECT r FROM Reservation r WHERE r.date = :date ORDER BY r.startTime")
    List<Reservation> findReservationsByDate(@Param("date") LocalDate date);

    @Query("SELECT r FROM Reservation r WHERE r.date = :date AND r.status = :status ORDER BY r.startTime")
    List<Reservation> findReservationsByDateAndStatus(@Param("date") LocalDate date, @Param("status") Reservation.Status status);

    @Query("SELECT SUM(FUNCTION('TIMESTAMPDIFF', MINUTE, r.startTime, r.endTime)) FROM Reservation r WHERE r.date = :date")
    Long sumReservedMinutesByDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(FUNCTION('TIMESTAMPDIFF', MINUTE, r.startTime, r.endTime)) FROM Reservation r WHERE r.date = :date AND r.status = :status")
    Long sumReservedMinutesByDateAndStatus(@Param("date") LocalDate date, @Param("status") Reservation.Status status);

    @Query("SELECT r FROM Reservation r WHERE r.createdAt >= :startOfDay AND r.createdAt < :endOfDay ORDER BY r.startTime")
    List<Reservation> findReservationsCreatedOnDate(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT r FROM Reservation r WHERE r.createdAt >= :startOfDay AND r.createdAt < :endOfDay AND r.status = :status ORDER BY r.startTime")
    List<Reservation> findReservationsCreatedOnDateAndStatus(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay, @Param("status") Reservation.Status status);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = :status")
    long countByStatus(@Param("status") Reservation.Status status);

    @Query("SELECT r FROM Reservation r WHERE LOWER(r.organizer.name) LIKE LOWER(CONCAT('%', :organizerName, '%')) ORDER BY r.date, r.startTime")
    List<Reservation> findByOrganizerNameContainingIgnoreCase(@Param("organizerName") String organizerName);

    @Query("SELECT r FROM Reservation r " +
           "LEFT JOIN FETCH r.organizer o " +
           "LEFT JOIN FETCH r.parentReservation p " +
           "WHERE (LOWER(o.name) LIKE LOWER(CONCAT('%', :organizerName, '%')) OR " +
           "(p IS NOT NULL AND LOWER(p.organizer.name) LIKE LOWER(CONCAT('%', :organizerName, '%')))) " +
           "ORDER BY COALESCE(p.id, r.id), r.date, r.startTime")
    List<Reservation> findByOrganizer(@Param("organizerName") String organizerName);

    @Query("SELECT r FROM Reservation r WHERE LOWER(r.organizer.name) LIKE LOWER(CONCAT('%', :organizerName, '%')) ORDER BY r.date, r.startTime")
    List<Reservation> findAllReservationsByOrganizerIncludingSeries(@Param("organizerName") String organizerName);

    List<Reservation> findByDateAndStatus(LocalDate date, Reservation.Status status);

    List<Reservation> findByDate(LocalDate date);

    List<Reservation> findByStatus(Reservation.Status status);

    @Query("SELECT r FROM Reservation r WHERE r.status = :status AND " +
            "((r.date = :fromDate AND r.startTime >= :fromTime) OR (r.date = :toDate AND r.startTime <= :toTime)) " +
            "AND r.reminderSentAt IS NULL")
    List<Reservation> findByStatusAndDateTimeRangeForReminder(
            @Param("status") Reservation.Status status,
            @Param("fromDate") LocalDate fromDate,
            @Param("fromTime") java.time.LocalTime fromTime,
            @Param("toDate") LocalDate toDate,
            @Param("toTime") java.time.LocalTime toTime);

    List<Reservation> findByParentReservation(Reservation parentReservation);

    @Query("SELECT r FROM Reservation r WHERE " +
           "r.date = :date AND " +
           "((r.startTime < :endTime AND r.endTime > :startTime) OR " +
           "(r.startTime = :startTime AND r.endTime = :endTime)) AND " +
           "r.status = 'CONFIRMED' AND " +
           "r.id != :excludeId")
    List<Reservation> findOverlappingReservations(
        @Param("date") LocalDate date,
        @Param("startTime") java.time.LocalTime startTime,
        @Param("endTime") java.time.LocalTime endTime,
        @Param("excludeId") Long excludeId
    );

    @Query("SELECT r FROM Reservation r WHERE " +
           "r.parentReservation = :parent AND " +
           "r.date >= :fromDate " +
           "ORDER BY r.date")
    List<Reservation> findFutureOccurrences(
        @Param("parent") Reservation parent,
        @Param("fromDate") LocalDate fromDate
    );
    @Query("SELECT r FROM Reservation r WHERE r.date >= :startDate AND r.date <= :endDate ORDER BY r.date, r.startTime")
    List<Reservation> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Reservation r WHERE r.date >= :startDate AND r.date <= :endDate AND r.status = :status ORDER BY r.date, r.startTime")
    List<Reservation> findByDateRangeAndStatus(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("status") Reservation.Status status);
}
