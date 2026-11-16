package com.admaal.resolvo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmbeddingTest {

  @Test
  void aceptaExactamente768Componentes() {
    Embedding embedding = new Embedding(new float[768]);

    assertThat(embedding.valores()).hasSize(768);
  }

  @Test
  void rechazaDimensionDistintaDe768() {
    assertThatThrownBy(() -> new Embedding(new float[767])).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Embedding(new float[769])).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rechazaComponentesNoFinitas() {
    float[] conNan = new float[768];
    conNan[10] = Float.NaN;
    float[] conInfinito = new float[768];
    conInfinito[0] = Float.POSITIVE_INFINITY;

    assertThatThrownBy(() -> new Embedding(conNan)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Embedding(conInfinito)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void noComparteElArrayDeEntradaNiDeSalida() {
    float[] original = new float[768];
    Embedding embedding = new Embedding(original);

    original[0] = 5f;
    embedding.valores()[1] = 7f;

    assertThat(embedding.valores()[0]).isZero();
    assertThat(embedding.valores()[1]).isZero();
  }
}
