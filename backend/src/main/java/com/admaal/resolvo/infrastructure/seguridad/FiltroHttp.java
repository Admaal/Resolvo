package com.admaal.resolvo.infrastructure.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class FiltroHttp {

  @Bean
  SecurityFilterChain cadena(HttpSecurity http, ObjectMapper json) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/sesion", "/api/sesion/refresh")
                    .permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/sesion")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}))
        .exceptionHandling(
            errores ->
                errores.authenticationEntryPoint(
                    (request, response, ex) -> {
                      response.setStatus(401);
                      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                      json.writeValue(response.getOutputStream(), Map.of("error", "no-autorizado"));
                    }));
    return http.build();
  }
}
