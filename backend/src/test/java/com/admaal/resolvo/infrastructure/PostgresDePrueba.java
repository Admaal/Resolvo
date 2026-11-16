package com.admaal.resolvo.infrastructure;

import java.time.Duration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class PostgresDePrueba {

  public static final String SECRETO = "resolvo-test-secret-key-32bytes!";

  @SuppressWarnings("resource")
  public static final PostgreSQLContainer<?> CONTENEDOR =
      new PostgreSQLContainer<>(
              DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
          .withStartupTimeout(Duration.ofMinutes(4));

  static {
    CONTENEDOR.start();
  }

  private PostgresDePrueba() {}

  public static void registrar(DynamicPropertyRegistry registro) {
    registro.add("spring.datasource.url", CONTENEDOR::getJdbcUrl);
    registro.add("spring.datasource.username", CONTENEDOR::getUsername);
    registro.add("spring.datasource.password", CONTENEDOR::getPassword);
    registro.add("resolvo.jwt.secret", () -> SECRETO);
    registro.add("resolvo.cookie.secure", () -> "false");
    registro.add("resolvo.ia.indexar-al-arrancar", () -> "false");
    registro.add("resolvo.ia.sincrona", () -> "true");
    // Cada contexto de Spring distinto guarda su pool; con el de 10 por defecto se agotan las 100 conexiones.
    registro.add("spring.datasource.hikari.maximum-pool-size", () -> "3");
  }
}
