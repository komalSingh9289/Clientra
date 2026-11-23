package com.clientra.ws;

import com.clientra.entity.User;
import com.clientra.repository.UserRepository;
import com.clientra.security.JwtService;
import io.jsonwebtoken.Claims;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Browsers cannot set an Authorization header on a WebSocket, so the app passes the JWT as
 * ?token=... . We verify it here during the handshake; a bad token means the socket is never opened.
 * (Because a token in a URL can end up in server logs, keep access logs off or masked in production.)
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtHandshakeInterceptor(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) return false;
        String token = ((ServletServerHttpRequest) request).getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) return false;
        try {
            Claims c = jwt.parse(token);
            Integer userId = Integer.valueOf(c.getSubject());
            User u = users.findById(userId).orElse(null);
            if (u == null || !u.isActive()) return false;
            attributes.put("userId", userId);          // handler reads this to know whose socket it is
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Exception ex) {
        // nothing to do
    }
}
