package com.admaal.resolvo.evals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

/** Lee los datasets JSONL (una linea, un caso) de {@code src/test/resources/evals}. */
public final class CargadorDeCasos {

  private static final ObjectMapper JSON = new ObjectMapper();

  private CargadorDeCasos() {}

  public static List<CasoClasificacion> clasificacion() {
    return leer("evals/clasificacion.jsonl", CasoClasificacion.class);
  }

  public static List<CasoRag> rag() {
    return leer("evals/rag.jsonl", CasoRag.class);
  }

  private static <T> List<T> leer(String ruta, Class<T> tipo) {
    List<T> casos = new ArrayList<>();
    try (BufferedReader lector =
        new BufferedReader(
            new InputStreamReader(new ClassPathResource(ruta).getInputStream(), StandardCharsets.UTF_8))) {
      String linea;
      while ((linea = lector.readLine()) != null) {
        if (!linea.isBlank()) {
          casos.add(JSON.readValue(linea, tipo));
        }
      }
    } catch (IOException error) {
      throw new UncheckedIOException(error);
    }
    return casos;
  }
}
