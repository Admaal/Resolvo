package com.admaal.resolvo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SugerenciaTest {

  private static final Instant AHORA = Instant.parse("2026-11-16T10:00:00Z");
  private final OrganizacionId org = new OrganizacionId(java.util.UUID.randomUUID());
  private final UsuarioId agente = new UsuarioId(java.util.UUID.randomUUID());
  private final FragmentoId f1 = FragmentoId.nuevo();
  private final FragmentoId f2 = FragmentoId.nuevo();

  private Sugerencia pendiente() {
    return Sugerencia.crear(
        SugerenciaId.nuevo(),
        org,
        TicketId.nuevo(),
        "Reinicia el router.",
        "gemini-test",
        AHORA,
        List.of(f1),
        Set.of(f1, f2));
  }

  @Test
  void nacePendienteConSusCitas() {
    Sugerencia sugerencia = pendiente();

    assertThat(sugerencia.estado()).isEqualTo(EstadoSugerencia.PENDIENTE);
    assertThat(sugerencia.citas()).extracting(Cita::fragmentoId).containsExactly(f1);
  }

  @Test
  void citaInventadaSeDescarta() {
    FragmentoId inventado = FragmentoId.nuevo();

    Sugerencia sugerencia =
        Sugerencia.crear(
            SugerenciaId.nuevo(),
            org,
            TicketId.nuevo(),
            "Texto",
            "m",
            AHORA,
            List.of(f1, inventado, f1, f2),
            Set.of(f1, f2));

    assertThat(sugerencia.citas()).extracting(Cita::fragmentoId).containsExactly(f1, f2);
    assertThat(sugerencia.citas()).extracting(Cita::orden).containsExactly(1, 2);
  }

  @Test
  void borradorVacioSeRechaza() {
    assertThatThrownBy(
            () ->
                Sugerencia.crear(
                    SugerenciaId.nuevo(), org, TicketId.nuevo(), " ", "m", AHORA, List.of(), Set.of()))
        .isInstanceOf(TextoInvalido.class);
  }

  @Test
  void aprobarDevuelveElTextoFinalYMarcaAprobada() {
    Sugerencia sugerencia = pendiente();

    String texto = sugerencia.aprobar(agente, Rol.AGENTE, "Texto editado", AHORA);

    assertThat(texto).isEqualTo("Texto editado");
    assertThat(sugerencia.estado()).isEqualTo(EstadoSugerencia.APROBADA);
    assertThat(sugerencia.resueltaPor()).isEqualTo(agente);
    assertThat(sugerencia.resueltaEn()).isEqualTo(AHORA);
  }

  @Test
  void clienteNoPuedeAprobar() {
    Sugerencia sugerencia = pendiente();

    assertThatThrownBy(() -> sugerencia.aprobar(agente, Rol.CLIENTE, "x", AHORA))
        .isInstanceOf(Prohibido.class);
    assertThat(sugerencia.estado()).isEqualTo(EstadoSugerencia.PENDIENTE);
  }

  @Test
  void clienteNoPuedeDescartar() {
    assertThatThrownBy(() -> pendiente().descartar(agente, Rol.CLIENTE, AHORA))
        .isInstanceOf(Prohibido.class);
  }

  @Test
  void aprobarDosVecesEs409() {
    Sugerencia sugerencia = pendiente();
    sugerencia.aprobar(agente, Rol.AGENTE, "uno", AHORA);

    assertThatThrownBy(() -> sugerencia.aprobar(agente, Rol.AGENTE, "dos", AHORA))
        .isInstanceOf(TransicionNoPermitida.class);
  }

  @Test
  void descartadaNoSePuedeAprobar() {
    Sugerencia sugerencia = pendiente();
    sugerencia.descartar(agente, Rol.ADMIN, AHORA);

    assertThat(sugerencia.estado()).isEqualTo(EstadoSugerencia.DESCARTADA);
    assertThatThrownBy(() -> sugerencia.aprobar(agente, Rol.AGENTE, "x", AHORA))
        .isInstanceOf(TransicionNoPermitida.class);
  }

  @Test
  void aprobadaNoSePuedeDescartar() {
    Sugerencia sugerencia = pendiente();
    sugerencia.aprobar(agente, Rol.AGENTE, "x", AHORA);

    assertThatThrownBy(() -> sugerencia.descartar(agente, Rol.AGENTE, AHORA))
        .isInstanceOf(TransicionNoPermitida.class);
  }

  @Test
  void textoFinalPasaPorTextoPlano() {
    Sugerencia sugerencia = pendiente();

    assertThatThrownBy(() -> sugerencia.aprobar(agente, Rol.AGENTE, "   ", AHORA))
        .isInstanceOf(TextoInvalido.class);
    assertThatThrownBy(() -> sugerencia.aprobar(agente, Rol.AGENTE, "x".repeat(8001), AHORA))
        .isInstanceOf(TextoInvalido.class);
    assertThat(sugerencia.estado()).isEqualTo(EstadoSugerencia.PENDIENTE);
  }

  @Test
  void fragmentoCalculaUnHashEstableDelTexto() {
    assertThat(Fragmento.hashDe("hola")).isEqualTo(Fragmento.hashDe("hola")).hasSize(64);
    assertThat(Fragmento.hashDe("hola")).isNotEqualTo(Fragmento.hashDe("adios"));
  }

  @Test
  void rehidratarConservaTodoElEstado() {
    SugerenciaId id = SugerenciaId.nuevo();
    TicketId ticket = TicketId.nuevo();
    Cita cita = new Cita(f1, 1);

    Sugerencia sugerencia =
        Sugerencia.rehidratar(
            id,
            org,
            ticket,
            "Borrador",
            "m",
            AHORA,
            List.of(cita),
            EstadoSugerencia.APROBADA,
            agente,
            AHORA.plusSeconds(5));

    assertThat(sugerencia.id()).isEqualTo(id);
    assertThat(sugerencia.organizacionId()).isEqualTo(org);
    assertThat(sugerencia.ticketId()).isEqualTo(ticket);
    assertThat(sugerencia.borrador()).isEqualTo("Borrador");
    assertThat(sugerencia.modelo()).isEqualTo("m");
    assertThat(sugerencia.creadaEn()).isEqualTo(AHORA);
    assertThat(sugerencia.citas()).containsExactly(cita);
    assertThat(sugerencia.estado()).isEqualTo(EstadoSugerencia.APROBADA);
    assertThat(sugerencia.resueltaPor()).isEqualTo(agente);
    assertThat(sugerencia.resueltaEn()).isEqualTo(AHORA.plusSeconds(5));
  }

  @Test
  void elFragmentoExponeElHashDeSuTexto() {
    Fragmento fragmento =
        new Fragmento(
            f1, org, FuenteConocimiento.ARTICULO, java.util.UUID.randomUUID(), "t", "contenido");

    assertThat(fragmento.hash()).isEqualTo(Fragmento.hashDe("contenido"));
  }
}
