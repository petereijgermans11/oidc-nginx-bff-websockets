package com.example.service.greeter;

import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class GreetingGenerator {

  List<String> greetings = List.of(
      "Bonjour",
      "Hola",
      "Zdravstvuyte",
      "Nǐn hǎo",
      "Salve",
      "Konnichiwa",
      "Guten Tag",
      "Olá",
      "Anyoung haseyo",
      "Goddag",
      "Goedendag",
      "Yassas",
      "Dzień dobry",
      "Selamat siang",
      "Namaste",
      "Sawasdee",
      "Merhaba");

  private static final Random RANDOM = new Random();

  public Greeting generateGreeting(String username) {
    return new Greeting(greetings.get(RANDOM.nextInt(greetings.size())) + " " + username + "!");
  }

  public Greeting generateGreeting() {
    return new Greeting(greetings.get(RANDOM.nextInt(greetings.size())) + "!");
  }
}
