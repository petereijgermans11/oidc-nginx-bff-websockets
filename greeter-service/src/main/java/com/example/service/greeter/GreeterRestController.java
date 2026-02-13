package com.example.service.greeter;

import com.example.service.greeter.security.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/greeter")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GreeterRestController {

  GreetingGenerator greetingGenerator;

  @GetMapping(value = "/greet", produces = MediaType.APPLICATION_JSON_VALUE)
  public Greeting greet() {
    log.info(
        "greet [Username: {}, Fullname: {}, Email: {}, Roles: {}]",
        User.getUsername(),
        User.getFullname(),
        User.getEmail().orElse("N/A"),
        User.getRoles());
    val username = User.getUsername();
    return greetingGenerator.generateGreeting(username);
  }
}
