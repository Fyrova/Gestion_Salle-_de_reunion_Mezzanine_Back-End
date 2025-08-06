package edbm.salle.demo.repository;

import edbm.salle.demo.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    @Query("SELECT e FROM EmailLog e WHERE (:status IS NULL OR e.status = :status) AND (:recipient IS NULL OR e.recipient LIKE %:recipient%)")
    List<EmailLog> findByStatusAndRecipient(@Param("status") String status, @Param("recipient") String recipient);
}
