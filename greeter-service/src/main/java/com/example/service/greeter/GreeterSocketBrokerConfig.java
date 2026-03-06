package com.example.service.greeter;

import com.example.service.greeter.security.WebSocketSessionRegistry;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class GreeterSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {

  private final WebSocketSessionRegistry sessionRegistry;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/ws/greeting");
    config.setApplicationDestinationPrefixes("/spring-security-mvc-socket");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws/greeter")
        .setAllowedOriginPatterns("*")
        .withSockJS()
        .setSessionCookieNeeded(false)
        .setHeartbeatTime(25000)
        .setDisconnectDelay(5000);
  }

  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
    registry.setMessageSizeLimit(128 * 1024);
    registry.setSendTimeLimit(20 * 1000);
    registry.setSendBufferSizeLimit(512 * 1024);

    registry.addDecoratorFactory(new WebSocketHandlerDecoratorFactory() {
      @Override
      public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
          @Override
          public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            Instant expiresAt = extractJwtExpiry(session);
            sessionRegistry.register(session, expiresAt);
            super.afterConnectionEstablished(session);
          }

          @Override
          public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus)
              throws Exception {
            sessionRegistry.remove(session.getId());
            super.afterConnectionClosed(session, closeStatus);
          }
        };
      }
    });
  }

  private static Instant extractJwtExpiry(WebSocketSession session) {
    if (session.getPrincipal() instanceof JwtAuthenticationToken jwtAuth) {
      return jwtAuth.getToken().getExpiresAt();
    }
    return null;
  }
}
