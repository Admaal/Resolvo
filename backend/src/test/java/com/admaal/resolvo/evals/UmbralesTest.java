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
    assertThat(umbrales.respuestaValida()).isGreaterThanOrEqualTo(0.95);
    // El plan pide una precision de citas >= 0,9: el borrador cita la fuente esperada.
    assertThat(umbrales.citaLaFuenteEsperada()).isGreaterThanOrEqualTo(0.9);
    assertThat(umbrales.tokensMediosPorLlamada()).isLessThanOrEqualTo(3000);
    // El p95 de tres corridas reales dio 8,6, 10,2 y 12,7 s (ADR-0011): 8 s no era realista y se
    // fijo en 15 s con esa evidencia. Subirlo otra vez exige cambiar el ADR, no solo este numero.
    assertThat(umbrales.p95Milisegundos()).isEqualTo(15000);
  }
}
