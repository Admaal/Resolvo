package com.admaal.resolvo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/** El umbral de distancia del RAG que se fijo con la primera corrida real de los evals (ADR-0011). */
class ConfiguracionRagTest {

  private static StandardEnvironment entornoDeApplicationYml() {
    StandardEnvironment entorno = new StandardEnvironment();
    try {
      List<PropertySource<?>> fuentes =
          new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
      fuentes.forEach(fuente -> entorno.getPropertySources().addLast(fuente));
    } catch (IOException error) {
      throw new UncheckedIOException(error);
    }
    return entorno;
  }

  @Test
  void laDistanciaMaximaPorDefectoYaNoDejaPasarCualquierFragmento() {
    Double distancia =
        entornoDeApplicationYml().getProperty("resolvo.rag.distancia-maxima", Double.class);

    // 2.0 es la distancia maxima posible: no descartaria nada. El eval separo las consultas con
    // respuesta (hasta 0,24) de las que no la tienen (desde 0,34) y recomendo 0,29.
    assertThat(distancia).isEqualTo(0.29);
  }
}
