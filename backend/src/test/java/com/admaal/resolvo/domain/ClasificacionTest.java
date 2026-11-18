package com.admaal.resolvo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClasificacionTest {

  private static final Instant AHORA = Instant.parse("2026-11-16T10:00:00Z");
  private final OrganizacionId org = new OrganizacionId(UUID.randomUUID());
  private final UsuarioId agente = new UsuarioId(UUID.randomUUID());
  private final CategoriaId categoria = new CategoriaId(UUID.randomUUID());

  private Clasificacion propuesta(String motivo) {
    return Clasificacion.proponer(
        ClasificacionId.nuevo(),
        org,
        TicketId.nuevo(),
        categoria,
        Prioridad.ALTA,
        motivo,
        "gemini-test",
        AHORA);
  }

  @Test
  void nacePendienteConLoPropuesto() {
    Clasificacion clasificacion = propuesta("El servicio esta caido");

    assertThat(clasificacion.estado()).isEqualTo(EstadoClasificacion.PENDIENTE);
    assertThat(clasificacion.categoriaId()).isEqualTo(categoria);
    assertThat(clasificacion.prioridad()).isEqualTo(Prioridad.ALTA);
    assertThat(clasificacion.motivo()).isEqualTo("El servicio esta caido");
    assertThat(clasificacion.modelo()).isEqualTo("gemini-test");
    assertThat(clasificacion.resueltaPor()).isNull();
  }

  @Test
  void aplicarLaResuelveConQuienYCuando() {
    Clasificacion clasificacion = propuesta("motivo");

    clasificacion.aplicar(agente, Rol.AGENTE, AHORA.plusSeconds(5));

    assertThat(clasificacion.estado()).isEqualTo(EstadoClasificacion.APLICADA);
    assertThat(clasificacion.resueltaPor()).isEqualTo(agente);
    assertThat(clasificacion.resueltaEn()).isEqualTo(AHORA.plusSeconds(5));
  }

  @Test
  void descartarLaResuelve() {
    Clasificacion clasificacion = propuesta("motivo");

    clasificacion.descartar(agente, Rol.ADMIN, AHORA);

    assertThat(clasificacion.estado()).isEqualTo(EstadoClasificacion.DESCARTADA);
  }

  @Test
  void aplicarDosVecesEsTransicionNoPermitida() {
    Clasificacion clasificacion = propuesta("motivo");
    clasificacion.aplicar(agente, Rol.AGENTE, AHORA);

    assertThatThrownBy(() -> clasificacion.aplicar(agente, Rol.AGENTE, AHORA))
        .isInstanceOf(TransicionNoPermitida.class);
    assertThatThrownBy(() -> clasificacion.descartar(agente, Rol.AGENTE, AHORA))
        .isInstanceOf(TransicionNoPermitida.class);
  }

  @Test
  void unClienteNoPuedeAplicarNiDescartar() {
    Clasificacion clasificacion = propuesta("motivo");

    assertThatThrownBy(() -> clasificacion.aplicar(agente, Rol.CLIENTE, AHORA))
        .isInstanceOf(Prohibido.class);
    assertThatThrownBy(() -> clasificacion.descartar(agente, Rol.CLIENTE, AHORA))
        .isInstanceOf(Prohibido.class);
    assertThat(clasificacion.estado()).isEqualTo(EstadoClasificacion.PENDIENTE);
  }

  @Test
  void elMotivoSeSaneaComoTextoPlano() {
    assertThatThrownBy(() -> propuesta(" ")).isInstanceOf(TextoInvalido.class);
    assertThatThrownBy(() -> propuesta("x".repeat(501))).isInstanceOf(TextoInvalido.class);
    assertThat(propuesta("x".repeat(500)).motivo()).hasSize(500);
  }

  @Test
  void rehidrataConSuEstado() {
    Clasificacion clasificacion =
        Clasificacion.rehidratar(
            ClasificacionId.nuevo(),
            org,
            TicketId.nuevo(),
            categoria,
            Prioridad.BAJA,
            "m",
            "modelo",
            AHORA,
            EstadoClasificacion.APLICADA,
            agente,
            AHORA);

    assertThat(clasificacion.estado()).isEqualTo(EstadoClasificacion.APLICADA);
    assertThat(clasificacion.resueltaPor()).isEqualTo(agente);
  }
}
