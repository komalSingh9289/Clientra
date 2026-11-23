package com.clientra.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends notification e-mails in the background (@Async, so a slow SMTP server never slows a request).
 * Disabled by default: with clientra.mail.enabled=false (or no SMTP host) it just writes the e-mail to
 * the log, which is handy during development.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> sender;
    private final boolean enabled;
    private final String from;

    public EmailService(ObjectProvider<JavaMailSender> sender,
                        @Value("${clientra.mail.enabled:false}") boolean enabled,
                        @Value("${clientra.mail.from:no-reply@clientra.local}") String from) {
        this.sender = sender;
        this.enabled = enabled;
        this.from = from;
    }

    @Async
    public void send(String to, String subject, String body) {
        JavaMailSender s = sender.getIfAvailable();
        if (!enabled || s == null || to == null || to.isBlank()) {
            log.info("[e-mail not sent: mail disabled] to={} subject={}", to, subject);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(body);
            s.send(m);
        } catch (Exception e) {
            log.warn("Could not send e-mail to {}: {}", to, e.getMessage());
        }
    }
}
