package com.example.service.greeter.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Thread-safe registry that tracks active WebSocket sessions together with the
 * expiration time of the JWT that was used during the HTTP handshake.
 */
@Slf4j
@Component
public class WebSocketSessionRegistry {

  private final ConcurrentHashMap<String, SessionEntry> sessions = new ConcurrentHashMap<>();

  public void register(WebSocketSession session, Instant jwtExpiresAt) {
    sessions.put(session.getId(), new SessionEntry(session, jwtExpiresAt));
    log.info("Registered WebSocket session {} (JWT expires at {})", session.getId(), jwtExpiresAt);
  }

  public void remove(String sessionId) {
    sessions.remove(sessionId);
    log.info("Removed WebSocket session {}", sessionId);
  }

  public Map<String, SessionEntry> getSessions() {
    return sessions;
  }

  public record SessionEntry(WebSocketSession session, Instant jwtExpiresAt) {}
}
