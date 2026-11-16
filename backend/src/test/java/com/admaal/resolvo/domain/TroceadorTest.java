package com.admaal.resolvo.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TroceadorTest {

  @Test
  void textoCortoEsUnFragmento() {
    assertThat(Troceador.trocear("Hola mundo")).containsExactly("Hola mundo");
  }

  @Test
  void textoVacioNoDaFragmentos() {
    assertThat(Troceador.trocear("   ")).isEmpty();
  }

  @Test
  void solapaYNoCortaPalabras() {
    String texto = "palabra ".repeat(300).trim();

    List<String> fragmentos = Troceador.trocear(texto);

    assertThat(fragmentos).hasSizeGreaterThan(1);
    assertThat(fragmentos).allSatisfy(f -> {
      assertThat(f.length()).isLessThanOrEqualTo(Troceador.TAMANO);
      assertThat(f).startsWith("palabra").endsWith("palabra");
    });
    String final0 = fragmentos.get(0);
    String inicio1 = fragmentos.get(1);
    String cola = final0.substring(final0.length() - 50);
    assertThat(inicio1).contains(cola.substring(cola.indexOf(' ') + 1));
  }

  @Test
  void mismoTextoMismosFragmentos() {
    String texto = "dato ".repeat(500);

    assertThat(Troceador.trocear(texto)).isEqualTo(Troceador.trocear(texto));
  }

  @Test
  void ningunaPalabraSeAPierdeEnElTextoCompleto() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 400; i++) {
      sb.append("w").append(i).append(' ');
    }

    String unido = String.join(" ", Troceador.trocear(sb.toString().trim())) + " ";

    for (int i = 0; i < 400; i++) {
      assertThat(unido).contains("w" + i + " ").as("w%d", i);
    }
  }

  private static String palabra(char c, int largo) {
    return String.valueOf(c).repeat(largo);
  }

  @Test
  void textoDeExactamenteElTamanoNoSeTrocea() {
    String texto = palabra('a', 399) + "\n" + palabra('b', 400);

    assertThat(texto).hasSize(800);
    assertThat(Troceador.trocear(texto)).containsExactly(texto);
  }

  @Test
  void unCaracterPorEncimaDelTamanoSeTroceaConSolapeDeUnaPalabra() {
    String[] palabras = new String[10];
    for (int i = 0; i < 9; i++) {
      palabras[i] = palabra((char) ('a' + i), 79);
    }
    palabras[9] = palabra('z', 81);
    String texto = String.join(" ", palabras);

    assertThat(texto).hasSize(801);
    assertThat(Troceador.trocear(texto))
        .containsExactly(
            String.join(" ", java.util.Arrays.copyOfRange(palabras, 0, 9)),
            palabras[8] + " " + palabras[9]);
  }

  @Test
  void elSolapeIncluyeUnaPalabraDeExactamenteElSolapeMenosElEspacio() {
    String[] palabras = new String[9];
    for (int i = 0; i < 9; i++) {
      palabras[i] = palabra((char) ('a' + i), 99);
    }

    assertThat(Troceador.trocear(String.join(" ", palabras)))
        .containsExactly(
            String.join(" ", java.util.Arrays.copyOfRange(palabras, 0, 8)),
            palabras[7] + " " + palabras[8]);
  }

  @Test
  void unaPalabraMasLargaQueElSolapeNoSeRepite() {
    String[] palabras = new String[9];
    for (int i = 0; i < 9; i++) {
      palabras[i] = palabra((char) ('a' + i), 100);
    }

    assertThat(Troceador.trocear(String.join(" ", palabras)))
        .containsExactly(
            String.join(" ", java.util.Arrays.copyOfRange(palabras, 0, 7)),
            palabras[7] + " " + palabras[8]);
  }

  @Test
  void unaPalabraMasLargaQueElTamanoSeParteEnTrozos() {
    String larga = palabra('x', 1700);

    assertThat(Troceador.trocear(larga + " fin"))
        .containsExactly(palabra('x', 800), palabra('x', 800), palabra('x', 100) + " fin");
  }

  @Test
  void elTextoNuloNoDaFragmentos() {
    assertThat(Troceador.trocear(null)).isEmpty();
  }
}
