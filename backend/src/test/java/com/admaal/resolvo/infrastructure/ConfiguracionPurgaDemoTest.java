package com.admaal.resolvo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** La limpieza de la demo solo existe con la propiedad puesta: apagada por defecto. */
@Tag("full")
class ConfiguracionPurgaDemoTest {

  @Tag("full")
  @SpringBootTest(classes = ResolvoApplication.class)
  static class SinPropiedad {

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registro) {
      PostgresDePrueba.registrar(registro);
    }

    @Autowired ApplicationContext contexto;

    @Test
    void noHayLimpiezaPorDefecto() {
      assertThat(contexto.getBeansOfType(PurgaDeDemo.class)).isEmpty();
    }
  }

  @Tag("full")
  @SpringBootTest(
      classes = ResolvoApplication.class,
      properties = "resolvo.demo.purgar-comentarios=true")
  static class ConPropiedad {

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registro) {
      PostgresDePrueba.registrar(registro);
    }

    @Autowired ApplicationContext contexto;

    @Test
    void conLaPropiedadHayLimpieza() {
      assertThat(contexto.getBeansOfType(PurgaDeDemo.class)).hasSize(1);
    }
  }
}
