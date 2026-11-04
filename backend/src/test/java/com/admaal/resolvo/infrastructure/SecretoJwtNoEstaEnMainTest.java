package com.admaal.resolvo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SecretoJwtNoEstaEnMainTest {

  @Test
  void elYamlDeMainNoTraeClaveDeFirma() throws IOException {
    Path recursos = Path.of("src/main/resources");
    try (Stream<Path> ficheros = Files.walk(recursos)) {
      ficheros
          .filter(Files::isRegularFile)
          .filter(ruta -> ruta.toString().endsWith(".yml") || ruta.toString().endsWith(".yaml") || ruta.toString().endsWith(".properties"))
          .forEach(SecretoJwtNoEstaEnMainTest::sinClave);
    }
  }

  private static void sinClave(Path ruta) {
    try {
      for (String linea : Files.readAllLines(ruta)) {
        if (linea.toLowerCase().contains("secret")) {
          assertThat(linea.trim()).isEqualTo("secret: ${RESOLVO_JWT_SECRET}");
        }
        assertThat(linea).doesNotContain("resolvo-test-secret-key-32bytes!");
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
