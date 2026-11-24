package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Comentario;
import com.admaal.resolvo.domain.ComentarioId;
import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.EventoTicket;
import com.admaal.resolvo.domain.MotivoModeracion;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.TipoEvento;
import com.admaal.resolvo.domain.UsuarioId;

/**
 * Ocultar (agente o admin) y restaurar (solo admin) un comentario. Nunca se borra. Si el ticket ya
 * esta resuelto o cerrado (y por tanto indexado en el RAG), un cambio real de visibilidad publica
 * {@code COMENTARIO_MODERADO} en la misma transaccion para que el indice se ponga al dia.
 */
public final class ModerarComentario {

  private final TicketRepositorio tickets;
  private final PublicadorEventos eventos;
  private final Reloj reloj;

  public ModerarComentario(TicketRepositorio tickets, PublicadorEventos eventos, Reloj reloj) {
    this.tickets = tickets;
    this.eventos = eventos;
    this.reloj = reloj;
  }

  public Comentario ocultar(
      OrganizacionId organizacionId,
      TicketId ticketId,
      ComentarioId comentarioId,
      UsuarioId actor,
      Rol rol,
      MotivoModeracion motivo) {
    Ticket ticket = visible(organizacionId, ticketId, actor, rol);
    boolean estabaRetirado = estaRetirado(ticket, comentarioId);
    Comentario oculto = ticket.ocultarComentario(comentarioId, actor, rol, motivo, reloj.ahora());
    tickets.guardar(ticket);
    publicarSiCambio(ticket, estabaRetirado, oculto);
    return oculto;
  }

  public Comentario restaurar(
      OrganizacionId organizacionId,
      TicketId ticketId,
      ComentarioId comentarioId,
      UsuarioId solicitante,
      Rol rol) {
    Ticket ticket = visible(organizacionId, ticketId, solicitante, rol);
    boolean estabaRetirado = estaRetirado(ticket, comentarioId);
    Comentario restaurado = ticket.restaurarComentario(comentarioId, rol);
    tickets.guardar(ticket);
    publicarSiCambio(ticket, estabaRetirado, restaurado);
    return restaurado;
  }

  /** Un ticket ajeno es indistinguible de uno inexistente: el 403 por rol solo llega al autor. */
  private Ticket visible(
      OrganizacionId organizacionId, TicketId ticketId, UsuarioId solicitante, Rol rol) {
    return tickets
        .porId(organizacionId, ticketId)
        .filter(ticket -> ticket.visiblePara(solicitante, rol))
        .orElseThrow(NoEncontrado::new);
  }

  private static boolean estaRetirado(Ticket ticket, ComentarioId comentarioId) {
    return ticket.todos().stream()
        .anyMatch(comentario -> comentario.id().equals(comentarioId) && comentario.retirado());
  }

  private void publicarSiCambio(Ticket ticket, boolean estabaRetirado, Comentario despues) {
    boolean indexado =
        ticket.estado() == EstadoTicket.RESUELTO || ticket.estado() == EstadoTicket.CERRADO;
    if (indexado && despues.retirado() != estabaRetirado) {
      eventos.publicar(
          EventoTicket.nuevo(
              TipoEvento.COMENTARIO_MODERADO, ticket.organizacionId(), ticket.id(), reloj.ahora()));
    }
  }
}
