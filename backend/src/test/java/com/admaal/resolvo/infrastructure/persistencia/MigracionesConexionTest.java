package com.admaal.resolvo.infrastructure.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Neon: la app sirve por el endpoint pooled y migra por la conexion directa (ADR-0009). */
@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
class MigracionesConexionTest {

  private static final String DIRECTA =
      PostgresDePrueba.CONTENEDOR.getJdbcUrl() + "&ApplicationName=migraciones";

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
    registro.add("RESOLVO_DB_MIGRATION_URL", () -> DIRECTA);
    registro.add("RESOLVO_DB_MIGRATION_USER", PostgresDePrueba.CONTENEDOR::getUsername);
    registro.add("RESOLVO_DB_MIGRATION_PASSWORD", PostgresDePrueba.CONTENEDOR::getPassword);
  }

  @Autowired Environment entorno;

  @Test
  void flywayUsaLaConexionDirectaCuandoSeConfigura() {
    assertThat(entorno.getProperty("spring.flyway.url")).isEqualTo(DIRECTA);
    assertThat(entorno.getProperty("spring.datasource.url")).isEqualTo(PostgresDePrueba.CONTENEDOR.getJdbcUrl());
  }
}
