package com.admaal.resolvo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketReclasificarTest {

  private static final Instant ABIERTO = Instant.parse("2026-11-16T10:00:00Z");
  private final OrganizacionId org = new OrganizacionId(UUID.randomUUID());
  private final Categoria consulta = new Categoria(new CategoriaId(UUID.randomUUID()), org, "Consulta");
  private final Categoria incidencia =
      new Categoria(new CategoriaId(UUID.randomUUID()), org, "Incidencia");
  private final PoliticaSla sla = new PoliticaSla();

  private Ticket ticket(EstadoTicket estado) {
    return Ticket.rehidratar(
        TicketId.nuevo(),
        org,
        new UsuarioId(UUID.randomUUID()),
        consulta.id(),
        Prioridad.BAJA,
        "RS-1",
        "Asunto",
        "Cuerpo",
        ABIERTO,
        ABIERTO.plus(sla.primeraRespuesta("Consulta", Prioridad.BAJA)),
        estado,
        null,
        List.of());
  }

  @Test
  void reclasificarRecalculaElVencimientoDesdeLaApertura() {
    Ticket ticket = ticket(EstadoTicket.ABIERTO);

    ticket.reclasificar(incidencia, Prioridad.URGENTE, Rol.AGENTE, sla);

    assertThat(ticket.categoriaId()).isEqualTo(incidencia.id());
    assertThat(ticket.prioridad()).isEqualTo(Prioridad.URGENTE);
    assertThat(ticket.venceEl()).isEqualTo(ABIERTO.plus(Duration.ofHours(1)));
  }

  @Test
  void unClienteNoPuedeReclasificar() {
    Ticket ticket = ticket(EstadoTicket.ABIERTO);

    assertThatThrownBy(() -> ticket.reclasificar(incidencia, Prioridad.URGENTE, Rol.CLIENTE, sla))
        .isInstanceOf(Prohibido.class);
    assertThat(ticket.prioridad()).isEqualTo(Prioridad.BAJA);
  }

  @Test
  void noSeReclasificaUnTicketResueltoOCerrado() {
    for (EstadoTicket estado : List.of(EstadoTicket.RESUELTO, EstadoTicket.CERRADO)) {
      Ticket ticket = ticket(estado);

      assertThatThrownBy(() -> ticket.reclasificar(incidencia, Prioridad.ALTA, Rol.AGENTE, sla))
          .isInstanceOf(TransicionNoPermitida.class);
      assertThat(ticket.categoriaId()).isEqualTo(consulta.id());
    }
  }

  @Test
  void laCategoriaDeOtraOrganizacionNoSePuedeAplicar() {
    Categoria ajena =
        new Categoria(
            new CategoriaId(UUID.randomUUID()), new OrganizacionId(UUID.randomUUID()), "Consulta");
    Ticket ticket = ticket(EstadoTicket.ABIERTO);

    assertThatThrownBy(() -> ticket.reclasificar(ajena, Prioridad.ALTA, Rol.AGENTE, sla))
        .isInstanceOf(NoEncontrado.class);
  }
}
