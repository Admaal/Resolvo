package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.application.Embebedor;
import com.admaal.resolvo.application.RedactorRespuestas;
import com.admaal.resolvo.application.soporte.EmbebedorFalso;
import com.admaal.resolvo.application.soporte.RedactorFalso;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Sustituye al proveedor de IA por fakes que los tests pueden inspeccionar y hacer fallar. */
@TestConfiguration
public class IaDeMentira {

  @Bean
  EmbebedorFalso embebedorFalso() {
    return new EmbebedorFalso();
  }

  @Bean
  RedactorFalso redactorFalso() {
    return new RedactorFalso();
  }

  @Bean
  @Primary
  Embebedor embebedor(EmbebedorFalso falso) {
    return falso;
  }

  @Bean
  @Primary
  RedactorRespuestas redactor(RedactorFalso falso) {
    return falso;
  }
}
