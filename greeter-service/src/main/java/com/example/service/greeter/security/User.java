package com.example.service.greeter.security;

import java.util.Collection;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

  class RealmAccess {
    Collection<String> roles;
  }

  public static String getUsername() {
    return ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication())
        .getToken()
        .getClaimAsString("preferred_username");
  }

  public static Collection<String> getRoles() {
    return ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication())
        .getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
  }

  public static String getFullname() {
    return ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication())
        .getToken()
        .getClaimAsString("name");
  }

  public static Optional<String> getEmail() {
    return ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication())
            .getToken()
            .getClaimAsBoolean("email_verified")
            .booleanValue()
        ? Optional.of(
            ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication())
                .getToken()
                .getClaimAsString("email"))
        : Optional.empty();
  }
}
