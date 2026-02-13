package com.example.service.greeter.security;

import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ExpressionJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
  @Value("${access-token.granted-authorities-claim}")
  private String grantedAuthoritiesClaim;

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
  return httpSecurity
      .csrf(AbstractHttpConfigurer::disable)
      .authorizeHttpRequests(requests -> requests
          .requestMatchers("/actuator/**")
          .permitAll()
          // REST + alle WebSocket endpoints vereisen ROLE_greeter
          .requestMatchers("/greeter/**", "/ws/**")
          .hasRole("greeter")
          .anyRequest()
          .denyAll())
      .oauth2ResourceServer(
          server -> server.jwt(c -> c.jwtAuthenticationConverter(jwtAuthenticationConverter())))
      .build();
}

  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    val result = new JwtAuthenticationConverter();
    result.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
    return result;
  }

  private ExpressionJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter() {
    val result = new ExpressionJwtGrantedAuthoritiesConverter(
        new SpelExpressionParser().parseRaw(grantedAuthoritiesClaim));
    result.setAuthorityPrefix("ROLE_");
    return result;
  }

  @Bean
  JwtDecoder jwtDecoder(
      @Value(value = "${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
      @Value(value = "${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
    val result = org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
        .build();
    result.setJwtValidator(
        org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer(issuerUri));
    return result;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    val result = new UrlBasedCorsConfigurationSource();
    result.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
    return result;
  }
}
