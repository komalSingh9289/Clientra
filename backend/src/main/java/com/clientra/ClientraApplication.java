package com.clientra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the Clientra backend.
 *
 * Request flow in one glance (read this first):
 *   HTTP request
 *     -> JwtAuthFilter            (security/)  verifies the Bearer token, loads the user
 *     -> Controller               (web/)       @PreAuthorize enforces the role (RBAC, synopsis 11.2)
 *     -> Service                  (service/)   business rules, data scope, audit trail
 *     -> Repository               (repository/) Spring Data JPA, parameterised SQL (synopsis 11.5)
 *     -> Database (H2 / MySQL)
 *   AI features live in ai/ and are called by the services.
 *   Reminders are dispatched by ReminderScheduler and pushed to the browser through ws/ (WebSocket).
 */
@SpringBootApplication
@EnableScheduling   // ReminderScheduler: due-reminder push, overdue e-mails, weekly digest
@EnableAsync        // EmailService sends mail in the background
public class ClientraApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClientraApplication.class, args);
    }
}
