package com.admaal.resolvo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TarifaTest {

  @Test
  void costeEsTokensPorPrecioPorMillon() {
    Tarifa tarifa = new Tarifa("m", new BigDecimal("0.30"), new BigDecimal("2.50"));

    assertThat(tarifa.coste(1_000_000, 0)).isEqualByComparingTo("0.30");
    assertThat(tarifa.coste(0, 1_000_000)).isEqualByComparingTo("2.50");
    assertThat(tarifa.coste(2_000, 1_000)).isEqualByComparingTo("0.0031");
    assertThat(tarifa.coste(0, 0)).isEqualByComparingTo("0");
  }

  @Test
  void unPrecioCeroEsValido() {
    Tarifa gratis = new Tarifa("m", BigDecimal.ZERO, BigDecimal.ZERO);

    assertThat(gratis.coste(1_000, 1_000)).isEqualByComparingTo("0");
    assertThat(new Tarifa("m", BigDecimal.ZERO, BigDecimal.ONE).coste(5, 0)).isEqualByComparingTo("0");
    assertThat(new Tarifa("m", BigDecimal.ONE, BigDecimal.ZERO).coste(0, 5)).isEqualByComparingTo("0");
  }

  @Test
  void unPrecioNegativoOUnModeloVacioNoSonUnaTarifa() {
    assertThatThrownBy(() -> new Tarifa("m", new BigDecimal("-1"), BigDecimal.ONE))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Tarifa("m", BigDecimal.ONE, new BigDecimal("-0.01")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Tarifa(" ", BigDecimal.ONE, BigDecimal.ONE))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
