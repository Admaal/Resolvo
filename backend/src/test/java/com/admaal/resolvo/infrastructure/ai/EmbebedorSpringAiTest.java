package com.admaal.resolvo.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.admaal.resolvo.application.IaNoDisponible;
import com.admaal.resolvo.domain.Embedding;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

class EmbebedorSpringAiTest {

  private final EmbeddingModel modelo = mock(EmbeddingModel.class);
  private final EmbebedorSpringAi embebedor = new EmbebedorSpringAi(modelo, "gemini-embedding-001");

  @Test
  void devuelveElEmbeddingDelModelo() {
    float[] vector = new float[Embedding.DIMENSION];
    vector[3] = 0.5f;
    when(modelo.embed("hola")).thenReturn(vector);

    Embedding resultado = embebedor.embeber("hola");

    assertThat(resultado.valores()[3]).isEqualTo(0.5f);
    assertThat(embebedor.modelo()).isEqualTo("gemini-embedding-001");
  }

  @Test
  void unaDimensionDistintaDe768EsUnFalloDelProveedor() {
    when(modelo.embed("hola")).thenReturn(new float[3072]);

    assertThatThrownBy(() -> embebedor.embeber("hola"))
        .isInstanceOf(IaNoDisponible.class)
        .hasMessage("ia-no-disponible");
  }

  @Test
  void unaCuotaAgotadaSeDistingueDeUnFallo() {
    when(modelo.embed("hola")).thenThrow(new RuntimeException("Error 429: quota exceeded"));

    assertThatThrownBy(() -> embebedor.embeber("hola"))
        .isInstanceOf(IaNoDisponible.class)
        .hasMessage("ia-cuota-agotada");
  }

  @Test
  void cualquierOtroErrorEsNoDisponible() {
    when(modelo.embed("hola")).thenThrow(new IllegalStateException("boom"));

    assertThatThrownBy(() -> embebedor.embeber("hola"))
        .isInstanceOf(IaNoDisponible.class)
        .hasMessage("ia-no-disponible");
  }
}
