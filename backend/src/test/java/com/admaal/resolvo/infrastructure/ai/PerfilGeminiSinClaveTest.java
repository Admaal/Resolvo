package com.admaal.resolvo.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Con el perfil gemini la clave es obligatoria: si falta, el arranque falla pronto y con un mensaje
 * que dice que variable poner, en vez de un error opaco del SDK a mitad de arranque.
 */
@Tag("full")
class PerfilGeminiSinClaveTest {

  @Test
  void sinClaveElArranqueFallaNombrandoLaVariable() {
    SpringApplicationBuilder aplicacion =
        new SpringApplicationBuilder(ResolvoApplication.class)
            .profiles("gemini")
            .web(WebApplicationType.NONE);

    assertThatThrownBy(
            () ->
                aplicacion.run(
                    "--RESOLVO_JWT_SECRET=resolvo-test-secret-key-32bytes!",
                    "--RESOLVO_GEMINI_API_KEY=",
                    "--spring.datasource.url=jdbc:postgresql://localhost:1/inexistente"))
        .hasStackTraceContaining("RESOLVO_GEMINI_API_KEY");
  }

  @Test
  void unaClaveEnBlancoTampocoVale() {
    SpringApplicationBuilder aplicacion =
        new SpringApplicationBuilder(ResolvoApplication.class)
            .profiles("gemini")
            .web(WebApplicationType.NONE);

    assertThatThrownBy(
            () ->
                aplicacion.run(
                    "--RESOLVO_JWT_SECRET=resolvo-test-secret-key-32bytes!",
                    "--RESOLVO_GEMINI_API_KEY=   ",
                    "--spring.datasource.url=jdbc:postgresql://localhost:1/inexistente"))
        .hasStackTraceContaining("RESOLVO_GEMINI_API_KEY");
  }
}
