package com.admaal.resolvo.evals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.core.io.ClassPathResource;

/** Los umbrales de calidad, coste y latencia, versionados en {@code evals/umbrales.json}. */
public record Umbrales(
    double categoriaAcierto,
    double prioridadExacta,
    double respuestaValida,
    double recallAtCinco,
    double sinRespuestaAcierto,
    double citaLaFuenteEsperada,
    double tokensMediosPorLlamada,
    long p95Milisegundos) {

  public static Umbrales cargar() {
    try (var entrada = new ClassPathResource("evals/umbrales.json").getInputStream()) {
      return new ObjectMapper().readValue(entrada, Umbrales.class);
    } catch (IOException error) {
      throw new UncheckedIOException(error);
    }
  }
}
