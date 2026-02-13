package com.example.service.greeter;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class GreeterSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/ws/greeting");
    config.setApplicationDestinationPrefixes("/spring-security-mvc-socket");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // Register SockJS endpoint with all transports enabled
    // Security is enforced at HTTP level (SecurityConfig) - no interceptors needed
    registry.addEndpoint("/ws/greeter")
        .setAllowedOriginPatterns("*") // Allow all origins for WebSocket
        .withSockJS()
        .setSessionCookieNeeded(false) // Don't require session cookies
        .setHeartbeatTime(25000) // Heartbeat interval
        .setDisconnectDelay(5000); // Disconnect delay
  }

  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
    // Configure WebSocket transport settings
    registry.setMessageSizeLimit(128 * 1024); // 128KB message size limit
    registry.setSendTimeLimit(20 * 1000); // 20 seconds send time limit
    registry.setSendBufferSizeLimit(512 * 1024); // 512KB send buffer size limit
  }
}
