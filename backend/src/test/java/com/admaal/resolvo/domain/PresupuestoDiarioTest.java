package com.admaal.resolvo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PresupuestoDiarioTest {

  private final PresupuestoDiario presupuesto = new PresupuestoDiario(3, 1000);

  @Test
  void admiteMientrasQuedeUnaLlamada() {
    assertThat(presupuesto.admite(new Consumo(0, 0), 0)).isTrue();
    assertThat(presupuesto.admite(new Consumo(2, 0), 0)).isTrue();
  }

  @Test
  void rechazaCuandoSeAlcanzaElLimiteDeLlamadas() {
    assertThat(presupuesto.admite(new Consumo(3, 0), 0)).isFalse();
    assertThat(presupuesto.admite(new Consumo(4, 0), 0)).isFalse();
  }

  @Test
  void rechazaCuandoSeAlcanzaElLimiteDeTokens() {
    assertThat(presupuesto.admite(new Consumo(0, 999), 0)).isTrue();
    assertThat(presupuesto.admite(new Consumo(0, 1000), 0)).isFalse();
    assertThat(presupuesto.admite(new Consumo(0, 5000), 0)).isFalse();
  }

  @Test
  void lasReservasEnCursoCuentanComoLlamadas() {
    assertThat(presupuesto.admite(new Consumo(2, 0), 0)).isTrue();
    assertThat(presupuesto.admite(new Consumo(2, 0), 1)).isFalse();
    assertThat(presupuesto.admite(new Consumo(0, 0), 2)).isTrue();
    assertThat(presupuesto.admite(new Consumo(0, 0), 3)).isFalse();
  }

  @Test
  void unLimiteNoPositivoSeRechaza() {
    assertThatThrownBy(() -> new PresupuestoDiario(0, 10)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PresupuestoDiario(10, 0)).isInstanceOf(IllegalArgumentException.class);
  }
}
