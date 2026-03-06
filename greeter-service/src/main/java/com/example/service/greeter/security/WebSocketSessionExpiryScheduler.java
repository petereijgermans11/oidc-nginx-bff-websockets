package com.example.service.greeter.security;

import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;

/**
 * Periodically iterates over all active WebSocket sessions and closes any
 * whose JWT has expired. This guarantees session termination even when no
 * inbound STOMP messages are flowing (server-push-only pattern).
 *
 * The STOMP client is configured with {@code reconnectDelay} so it will
 * automatically reconnect after the close, obtaining a fresh token from
 * the BFF during the new HTTP handshake.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionExpiryScheduler {

  private static final CloseStatus JWT_EXPIRED =
      new CloseStatus(4401, "JWT expired");

  private final WebSocketSessionRegistry registry;

  @Scheduled(fixedRate = 5000)
  public void closeExpiredSessions() {
    Instant now = Instant.now();

    registry.getSessions().forEach((sessionId, entry) -> {
      if (entry.jwtExpiresAt() != null && now.isAfter(entry.jwtExpiresAt())) {
        log.info(
            "Closing WebSocket session {} because JWT expired at {} (now={})",
            sessionId,
            entry.jwtExpiresAt(),
            now);
        try {
          entry.session().close(JWT_EXPIRED);
        } catch (IOException e) {
          log.warn("Error closing WebSocket session {}: {}", sessionId, e.getMessage());
        }
        registry.remove(sessionId);
      }
    });
  }
}
