package com.admaal.resolvo.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TicketRehidratadoTest {

  @Test
  void conservaEstadoAsignadoYComentarios() {
    Instant ahora = Instant.parse("2026-11-04T10:00:00Z");
    OrganizacionId organizacion = new OrganizacionId(java.util.UUID.randomUUID());
    UsuarioId autor = new UsuarioId(java.util.UUID.randomUUID());
    UsuarioId agente = new UsuarioId(java.util.UUID.randomUUID());
    Comentario nota =
        new Comentario(ComentarioId.nuevo(), agente, "nota interna", true, ahora);

    Ticket ticket =
        Ticket.rehidratar(
            TicketId.nuevo(),
            organizacion,
            autor,
            new CategoriaId(java.util.UUID.randomUUID()),
            Prioridad.ALTA,
            "RS-4",
            "Asunto",
            "Cuerpo",
            ahora,
            ahora.plusSeconds(3600),
            EstadoTicket.PENDIENTE,
            agente,
            List.of(nota));

    assertThat(ticket.estado()).isEqualTo(EstadoTicket.PENDIENTE);
    assertThat(ticket.asignadoId()).isEqualTo(agente);
    assertThat(ticket.codigo()).isEqualTo("RS-4");
    assertThat(ticket.visiblesPara(Rol.AGENTE)).containsExactly(nota);
    assertThat(ticket.visiblesPara(Rol.CLIENTE)).isEmpty();
  }
}
