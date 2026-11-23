package com.clientra.ws;

import com.clientra.dto.Dtos.NotificationDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Registry of open WebSocket connections, keyed by user id. Any service can call send(userId, ...)
 * to push a real-time notification to that user's open browser tabs (synopsis Module 6).
 */
@Component
public class NotificationHub {
    private static final Logger log = LoggerFactory.getLogger(NotificationHub.class);

    private final Map<Integer, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public NotificationHub(ObjectMapper mapper) { this.mapper = mapper; }

    public void register(Integer userId, WebSocketSession s) {
        sessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(s);
    }

    public void unregister(Integer userId, WebSocketSession s) {
        Set<WebSocketSession> set = sessions.get(userId);
        if (set != null) set.remove(s);
    }

    /** Pushes to every open tab of that user; silently does nothing if they are offline. */
    public void send(Integer userId, NotificationDto n) {
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null || set.isEmpty()) return;
        try {
            TextMessage msg = new TextMessage(mapper.writeValueAsString(n));
            for (WebSocketSession s : set) {
                if (!s.isOpen()) { set.remove(s); continue; }
                synchronized (s) {                       // WebSocketSession.sendMessage is not thread-safe
                    try { s.sendMessage(msg); } catch (IOException e) { log.debug("push failed: {}", e.getMessage()); }
                }
            }
        } catch (Exception e) {
            log.warn("Could not serialise notification: {}", e.getMessage());
        }
    }
}
