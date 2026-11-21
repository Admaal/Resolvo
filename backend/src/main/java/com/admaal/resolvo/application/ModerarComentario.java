package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Comentario;
import com.admaal.resolvo.domain.ComentarioId;
import com.admaal.resolvo.domain.MotivoModeracion;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.UsuarioId;

/** Ocultar (agente o admin) y restaurar (solo admin) un comentario. Nunca se borra. */
public final class ModerarComentario {

  private final TicketRepositorio tickets;
  private final Reloj reloj;

  public ModerarComentario(TicketRepositorio tickets, Reloj reloj) {
    this.tickets = tickets;
    this.reloj = reloj;
  }

  public Comentario ocultar(
      OrganizacionId organizacionId,
      TicketId ticketId,
      ComentarioId comentarioId,
      UsuarioId actor,
      Rol rol,
      MotivoModeracion motivo) {
    Ticket ticket = tickets.porId(organizacionId, ticketId).orElseThrow(NoEncontrado::new);
    Comentario oculto = ticket.ocultarComentario(comentarioId, actor, rol, motivo, reloj.ahora());
    tickets.guardar(ticket);
    return oculto;
  }

  public Comentario restaurar(
      OrganizacionId organizacionId, TicketId ticketId, ComentarioId comentarioId, Rol rol) {
    Ticket ticket = tickets.porId(organizacionId, ticketId).orElseThrow(NoEncontrado::new);
    Comentario restaurado = ticket.restaurarComentario(comentarioId, rol);
    tickets.guardar(ticket);
    return restaurado;
  }
}
