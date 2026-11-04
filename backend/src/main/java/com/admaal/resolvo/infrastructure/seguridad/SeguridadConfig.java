package com.admaal.resolvo.infrastructure.seguridad;

import com.admaal.resolvo.application.Reloj;
import com.admaal.resolvo.application.VerificadorContrasena;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableConfigurationProperties(SeguridadConfig.CookiePropiedades.class)
public class SeguridadConfig {

  @Bean
  SecretKey claveJwt(@Value("${resolvo.jwt.secret}") String secreto) {
    byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      throw new IllegalStateException("RESOLVO_JWT_SECRET corta");
    }
    return new SecretKeySpec(bytes, "HmacSHA256");
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKey clave) {
    return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  VerificadorContrasena verificador(PasswordEncoder encoder) {
    return encoder::matches;
  }

  @Bean
  Reloj reloj() {
    return Instant::now;
  }

  @ConfigurationProperties(prefix = "resolvo.cookie")
  public record CookiePropiedades(boolean secure) {}
}
