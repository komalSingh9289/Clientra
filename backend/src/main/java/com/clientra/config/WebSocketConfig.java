package com.clientra.config;

import com.clientra.ws.JwtHandshakeInterceptor;
import com.clientra.ws.NotificationWebSocketHandler;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Registers the real-time notification endpoint: ws://host:8080/ws/notifications?token=<JWT> */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final NotificationWebSocketHandler handler;
    private final JwtHandshakeInterceptor interceptor;
    private final List<String> origins;

    public WebSocketConfig(NotificationWebSocketHandler handler, JwtHandshakeInterceptor interceptor,
                           @Value("${clientra.cors.allowed-origins}") List<String> origins) {
        this.handler = handler;
        this.interceptor = interceptor;
        this.origins = origins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/notifications")
                .addInterceptors(interceptor)
                .setAllowedOrigins(origins.toArray(new String[0]));   // same origin whitelist as CORS
    }
}
