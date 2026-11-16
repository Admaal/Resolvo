package com.admaal.resolvo.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.IaNoDisponible;
import java.util.List;
import org.junit.jupiter.api.Test;

class IaDesactivadaTest {

  private final IaDesactivada ia = new IaDesactivada();

  @Test
  void embeberDiceQueLaIaNoEstaConfigurada() {
    assertThatThrownBy(() -> ia.embeber("hola"))
        .isInstanceOf(IaNoDisponible.class)
        .hasMessage("ia-no-configurada");
  }

  @Test
  void redactarDiceQueLaIaNoEstaConfigurada() {
    assertThatThrownBy(() -> ia.redactar(null, List.of()))
        .isInstanceOf(IaNoDisponible.class)
        .hasMessage("ia-no-configurada");
  }

  @Test
  void clasificarDiceQueLaIaNoEstaConfigurada() {
    assertThatThrownBy(() -> ia.clasificar(null, List.of()))
        .isInstanceOf(IaNoDisponible.class)
        .hasMessage("ia-no-configurada");
  }

  @Test
  void anunciaUnModeloVacio() {
    assertThat(ia.modelo()).isEqualTo("desactivada");
  }
}
