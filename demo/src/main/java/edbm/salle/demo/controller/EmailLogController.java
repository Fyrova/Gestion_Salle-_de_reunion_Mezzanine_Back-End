package edbm.salle.demo.controller;

import edbm.salle.demo.model.EmailLog;
import edbm.salle.demo.repository.EmailLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email-logs")
public class EmailLogController {

    @Autowired
    private EmailLogRepository emailLogRepository;

    @GetMapping
    public List<EmailLog> getAllEmailLogs(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String recipient) {
        return emailLogRepository.findByStatusAndRecipient(status, recipient);
    }

    @GetMapping("/{id}")
    public EmailLog getEmailLogById(@PathVariable Long id) {
        return emailLogRepository.findById(id).orElse(null);
    }

    // Additional filtering endpoints can be added here (by recipient, status, date, etc.)
}
