package com.admaal.resolvo.infrastructure.ai;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;

/** Prompts versionados en recursos, con marcadores {{nombre}} y texto del cliente sin delimitadores. */
final class PlantillaDePrompt {

  private static final Pattern DELIMITADOR =
      Pattern.compile("<\\s*/?\\s*ticket\\s*>", Pattern.CASE_INSENSITIVE);
  private static final Pattern MARCADOR = Pattern.compile("\\{\\{(\\w+)}}");

  private final String plantilla;

  private PlantillaDePrompt(String plantilla) {
    this.plantilla = plantilla;
  }

  static PlantillaDePrompt cargar(String ruta) {
    try (var entrada = new ClassPathResource(ruta).getInputStream()) {
      return new PlantillaDePrompt(new String(entrada.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IOException error) {
      throw new UncheckedIOException(error);
    }
  }

  String rellenar(Map<String, String> valores) {
    // Una sola pasada: el texto insertado no vuelve a pasar por el reemplazo de marcadores.
    Matcher marcador = MARCADOR.matcher(plantilla);
    StringBuilder prompt = new StringBuilder();
    while (marcador.find()) {
      marcador.appendReplacement(
          prompt, Matcher.quoteReplacement(valores.getOrDefault(marcador.group(1), marcador.group())));
    }
    marcador.appendTail(prompt);
    return prompt.toString();
  }

  /** Quita los delimitadores que cerrarian el bloque de datos del cliente y abririan instrucciones. */
  static String sinDelimitadores(String texto) {
    // Se repite hasta que no cambie: quitar uno puede juntar los trozos de otro ("<tic<ticket>ket>").
    String actual = texto;
    String anterior;
    do {
      anterior = actual;
      actual = DELIMITADOR.matcher(anterior).replaceAll("");
    } while (!actual.equals(anterior));
    return actual;
  }
}
