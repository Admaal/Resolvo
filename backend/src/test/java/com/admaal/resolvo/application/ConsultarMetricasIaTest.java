package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.RegistroLlamadasEnMemoria;
import com.admaal.resolvo.domain.LlamadaIa;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.ResumenLlamadas;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Tarifa;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarMetricasIaTest {

  private static final Instant DESDE = Instant.parse("2026-11-12T00:00:00Z");
  private static final Instant HASTA = Instant.parse("2026-11-19T00:00:00Z");
  private static final Instant DENTRO = Instant.parse("2026-11-15T10:00:00Z");

  private final OrganizacionId acme = new OrganizacionId(UUID.randomUUID());
  private final OrganizacionId otra = new OrganizacionId(UUID.randomUUID());
  private final RegistroLlamadasEnMemoria registro = new RegistroLlamadasEnMemoria();
  private final Tarifas tarifas =
      modelo ->
          "m1".equals(modelo)
              ? Optional.of(new Tarifa("m1", new BigDecimal("1"), new BigDecimal("2")))
              : Optional.empty();
  private final ConsultarMetricasIa consulta = new ConsultarMetricasIa(registro, tarifas);

  private void llamada(OrganizacionId org, Instant cuando, int entrada) {
    registro.registrar(new LlamadaIa(UUID.randomUUID(), org, "sugerir", "m1", entrada, 0, 100, true, cuando));
  }

  @Test
  void elAdminVeElResumenDeSuOrganizacionConLaTarifaAplicada() {
    llamada(acme, DENTRO, 1_000_000);

    ResumenLlamadas resumen = consulta.ejecutar(acme, Rol.ADMIN, DESDE, HASTA);

    assertThat(resumen.llamadas()).isEqualTo(1);
    assertThat(resumen.costeUsd()).isEqualByComparingTo("1");
  }

  @Test
  void nuncaIncluyeLlamadasDeOtraOrganizacion() {
    llamada(acme, DENTRO, 10);
    llamada(otra, DENTRO, 999);

    ResumenLlamadas resumen = consulta.ejecutar(acme, Rol.ADMIN, DESDE, HASTA);

    assertThat(resumen.llamadas()).isEqualTo(1);
    assertThat(resumen.tokensEntrada()).isEqualTo(10);
  }

  @Test
  void soloCuentaLasLlamadasDentroDelRango() {
    llamada(acme, DESDE.minusSeconds(1), 1);
    llamada(acme, DESDE, 2);
    llamada(acme, HASTA.minusSeconds(1), 4);
    llamada(acme, HASTA, 8);

    ResumenLlamadas resumen = consulta.ejecutar(acme, Rol.ADMIN, DESDE, HASTA);

    assertThat(resumen.tokensEntrada()).isEqualTo(6);
  }

  @Test
  void agenteYClienteNoPuedenVerLasMetricas() {
    llamada(acme, DENTRO, 10);

    assertThatThrownBy(() -> consulta.ejecutar(acme, Rol.AGENTE, DESDE, HASTA))
        .isInstanceOf(Prohibido.class);
    assertThatThrownBy(() -> consulta.ejecutar(acme, Rol.CLIENTE, DESDE, HASTA))
        .isInstanceOf(Prohibido.class);
  }

  @Test
  void unRangoAlReves_noSeAceptaSilenciosamente() {
    assertThatThrownBy(() -> consulta.ejecutar(acme, Rol.ADMIN, HASTA, DESDE))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
