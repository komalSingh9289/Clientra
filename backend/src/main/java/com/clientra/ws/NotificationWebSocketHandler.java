package com.clientra.ws;

import com.clientra.dto.Dtos.NotificationDto;
import com.clientra.entity.Lead;
import com.clientra.entity.Reminder;
import com.clientra.entity.ReminderStatus;
import com.clientra.repository.LeadRepository;
import com.clientra.repository.ReminderRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Lifecycle of one browser connection to /ws/notifications:
 *   connect  -> remember the socket for that user, and immediately push the reminders that are already due
 *               (so the bell is filled right after a page reload)
 *   later    -> ReminderScheduler / ActivityService push through NotificationHub
 *   close    -> forget the socket
 * The browser never sends anything, so incoming messages are ignored.
 */
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    private final NotificationHub hub;
    private final ReminderRepository reminders;
    private final LeadRepository leads;

    public NotificationWebSocketHandler(NotificationHub hub, ReminderRepository reminders, LeadRepository leads) {
        this.hub = hub;
        this.reminders = reminders;
        this.leads = leads;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId == null) return;
        hub.register(userId, session);
        for (Reminder r : reminders.findByUserIdAndStatusAndReminderTimeLessThanEqual(userId, ReminderStatus.Pending, Instant.now())) {
            Lead lead = leads.findById(r.getLeadId()).orElse(null);
            hub.send(userId, new NotificationDto("rem-" + r.getReminderId(),
                    "Follow-up due: " + (lead == null ? "a lead" : lead.getFullName()), r.getMessage(), r.getLeadId(), r.getReminderTime()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId != null) hub.unregister(userId, session);
    }
}
