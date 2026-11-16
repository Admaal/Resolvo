package com.admaal.resolvo.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.application.Embebedor;
import com.admaal.resolvo.application.RedactorRespuestas;
import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** El perfil gemini arranca con una clave falsa y no hace ninguna llamada de red al crearse. */
@Tag("full")
@ActiveProfiles("gemini")
@SpringBootTest(classes = ResolvoApplication.class)
class PerfilGeminiTest {

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
    registro.add("RESOLVO_GEMINI_API_KEY", () -> "clave-falsa-de-prueba");
  }

  @Autowired Embebedor embebedor;
  @Autowired RedactorRespuestas redactor;

  @Test
  void conElPerfilGeminiSeUsanLosAdaptersDeSpringAi() {
    assertThat(embebedor).isInstanceOf(EmbebedorSpringAi.class);
    assertThat(redactor).isInstanceOf(RedactorSpringAi.class);
    assertThat(embebedor.modelo()).isEqualTo("gemini-embedding-001");
    assertThat(redactor.modelo()).isEqualTo("gemini-2.5-flash");
  }
}
