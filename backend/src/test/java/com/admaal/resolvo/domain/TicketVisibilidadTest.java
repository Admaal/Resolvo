package com.admaal.resolvo.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketVisibilidadTest {

  private static final Instant ABIERTO = Instant.parse("2026-11-24T10:00:00Z");
  private final UsuarioId autor = new UsuarioId(UUID.randomUUID());
  private final UsuarioId otro = new UsuarioId(UUID.randomUUID());
  private final Ticket ticket =
      Ticket.rehidratar(
          TicketId.nuevo(),
          new OrganizacionId(UUID.randomUUID()),
          autor,
          new CategoriaId(UUID.randomUUID()),
          Prioridad.BAJA,
          "RS-1",
          "Asunto",
          "Cuerpo",
          ABIERTO,
          ABIERTO.plusSeconds(3600),
          EstadoTicket.ABIERTO,
          null,
          List.of());

  @Test
  void unClienteVeSuPropioTicket() {
    assertThat(ticket.visiblePara(autor, Rol.CLIENTE)).isTrue();
  }

  @Test
  void unClienteNoVeElTicketDeOtro() {
    assertThat(ticket.visiblePara(otro, Rol.CLIENTE)).isFalse();
  }

  @Test
  void unAgenteYUnAdminVenTicketsDeCualquiera() {
    assertThat(ticket.visiblePara(otro, Rol.AGENTE)).isTrue();
    assertThat(ticket.visiblePara(otro, Rol.ADMIN)).isTrue();
  }

  @Test
  void unTicketAsignadoAUnClienteNoLeDaAcceso() {
    Ticket asignado =
        Ticket.rehidratar(
            TicketId.nuevo(),
            new OrganizacionId(UUID.randomUUID()),
            autor,
            new CategoriaId(UUID.randomUUID()),
            Prioridad.BAJA,
            "RS-2",
            "Asunto",
            "Cuerpo",
            ABIERTO,
            ABIERTO.plusSeconds(3600),
            EstadoTicket.ABIERTO,
            otro,
            List.of());

    assertThat(asignado.visiblePara(otro, Rol.CLIENTE)).isFalse();
  }
}
