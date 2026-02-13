package com.example.service.greeter;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@EnableScheduling
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GreeterSocketController {

  GreetingGenerator greetingGenerator;
  SimpMessagingTemplate messagingTemplate;

  @Scheduled(fixedRateString = "${greeting-socket.refresh-rate-ms}", initialDelay = 0)
  public void generateNewGreeting() {
    Greeting greeting = greetingGenerator.generateGreeting();
    log.info("Sending greeting via WebSocket: {}", greeting);
    // Send greeting directly to the /ws/greeting topic
    messagingTemplate.convertAndSend("/ws/greeting", greeting);
  }

  @MessageMapping("/ws/greeter")
  @SendTo("/ws/greeting")
  public Greeting send(Greeting greeting) {
    log.info("Received and forwarding greeting: {}", greeting);
    return greeting;
  }
}
