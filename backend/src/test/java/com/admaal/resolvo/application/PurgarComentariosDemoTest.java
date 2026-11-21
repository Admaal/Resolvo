package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.application.soporte.RelojManual;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PurgarComentariosDemoTest {

  @Test
  void borraLoCreadoAntesDeLasUltimasVeinticuatroHoras() {
    Instant ahora = Instant.parse("2026-11-21T10:00:00Z");
    List<Instant> limites = new ArrayList<>();
    ComentariosDemo comentarios =
        limite -> {
          limites.add(limite);
          return 3;
        };

    int borrados = new PurgarComentariosDemo(comentarios, new RelojManual(ahora)).ejecutar();

    assertThat(limites).containsExactly(Instant.parse("2026-11-20T10:00:00Z"));
    assertThat(borrados).isEqualTo(3);
  }
}
