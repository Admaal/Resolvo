package com.admaal.resolvo.evals;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class UmbralesTest {

  private final Umbrales umbrales = Umbrales.cargar();

  @Test
  void lasProporcionesEstanEntreCeroYUnoSinIncluirElCero() {
    assertThat(
            List.of(
                umbrales.categoriaAcierto(),
                umbrales.prioridadExacta(),
                umbrales.respuestaValida(),
                umbrales.recallAtCinco(),
                umbrales.sinRespuestaAcierto(),
                umbrales.citaLaFuenteEsperada()))
        .allSatisfy(valor -> assertThat(valor).isGreaterThan(0.0).isLessThanOrEqualTo(1.0));
  }

  @Test
  void costeYLatenciaSonPositivos() {
    assertThat(umbrales.tokensMediosPorLlamada()).isPositive();
    assertThat(umbrales.p95Milisegundos()).isPositive();
  }

  @Test
  void elUmbralDeCalidadNoEstaRebajadoPorDebajoDelMinimoAcordado() {
    // Bajar un umbral para que pase cuenta como debilitar un test (AGENTS.md): estos suelos
    // son los acordados en el plan de la fase 4 y solo se cambian con un cambio de ADR.
    assertThat(umbrales.categoriaAcierto()).isGreaterThanOrEqualTo(0.9);
    assertThat(umbrales.prioridadExacta()).isGreaterThanOrEqualTo(0.7);
    assertThat(umbrales.recallAtCinco()).isGreaterThanOrEqualTo(0.8);
    assertThat(umbrales.sinRespuestaAcierto()).isGreaterThanOrEqualTo(0.8);
    assertThat(umbrales.p95Milisegundos()).isLessThanOrEqualTo(8000);
  }
}
