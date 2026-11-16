package com.admaal.resolvo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** La clave de Gemini solo llega por variable de entorno: ni en el yaml ni en el codigo de main. */
class SecretoGeminiNoEstaEnMainTest {

  private static final String CLAVE_PERMITIDA = "api-key: ${RESOLVO_GEMINI_API_KEY:}";

  @Test
  void losFicherosDeConfiguracionSoloLeenLaClaveDeLaVariableDeEntorno() throws IOException {
    for (Path ruta : ficheros("src/main/resources")) {
      assertThat(infracciones(Files.readAllLines(ruta))).as(ruta.toString()).isEmpty();
    }
  }

  @Test
  void elCodigoDeMainNoLlevaNingunaClaveDeGoogle() throws IOException {
    for (Path ruta : ficheros("src/main")) {
      assertThat(Files.readString(ruta)).as(ruta.toString()).doesNotContain("AIza");
    }
  }

  @Test
  void elComprobadorDetectaUnaClaveEscrita() {
    assertThat(infracciones(List.of("api-key: AIzaSyEjemploDeClaveEscrita1234567890abcd"))).hasSize(1);
    assertThat(infracciones(List.of("        api-key: mi-clave"))).hasSize(1);
    assertThat(infracciones(List.of("        api-key: ${RESOLVO_GEMINI_API_KEY:}"))).isEmpty();
    assertThat(infracciones(List.of("api-key: ${RESOLVO_GEMINI_API_KEY}"))).hasSize(1);
  }

  private static List<String> infracciones(List<String> lineas) {
    return lineas.stream()
        .map(String::trim)
        .filter(linea -> linea.toLowerCase().contains("api-key") || linea.contains("AIza"))
        .filter(linea -> !linea.equals(CLAVE_PERMITIDA))
        .toList();
  }

  private static List<Path> ficheros(String raiz) throws IOException {
    try (Stream<Path> todos = Files.walk(Path.of(raiz))) {
      return todos.filter(Files::isRegularFile).toList();
    }
  }
}
