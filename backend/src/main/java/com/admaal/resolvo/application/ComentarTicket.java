package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Comentario;
import com.admaal.resolvo.domain.ComentarioId;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.UsuarioId;

public final class ComentarTicket {

  private final TicketRepositorio tickets;
  private final Reloj reloj;

  public ComentarTicket(TicketRepositorio tickets, Reloj reloj) {
    this.tickets = tickets;
    this.reloj = reloj;
  }

  public Comentario ejecutar(
      OrganizacionId organizacionId,
      TicketId ticketId,
      UsuarioId autorId,
      Rol rol,
      String cuerpo,
      boolean interno) {
    // Un cliente solo comenta en sus tickets; en un ticket ajeno recibe lo mismo que si no existiera.
    Ticket ticket =
        tickets
            .porId(organizacionId, ticketId)
            .filter(visible -> visible.visiblePara(autorId, rol))
            .orElseThrow(NoEncontrado::new);
    Comentario comentario =
        ticket.comentar(ComentarioId.nuevo(), autorId, rol, cuerpo, interno, reloj.ahora());
    tickets.guardar(ticket);
    return comentario;
  }
}
