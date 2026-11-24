package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Comentario;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.UsuarioId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ConsultarTicket {

  private final TicketRepositorio tickets;

  public ConsultarTicket(TicketRepositorio tickets) {
    this.tickets = tickets;
  }

  /** Un ticket que el solicitante no puede ver es indistinguible de uno que no existe. */
  public Ticket ejecutar(
      OrganizacionId organizacionId, TicketId ticketId, UsuarioId solicitante, Rol rol) {
    return visible(tickets.porId(organizacionId, ticketId), solicitante, rol);
  }

  /** Abre un ticket por su codigo visible ({@code RS-3}), sin distinguir mayusculas. */
  public Ticket porCodigo(
      OrganizacionId organizacionId, String codigo, UsuarioId solicitante, Rol rol) {
    return visible(
        tickets.porCodigo(organizacionId, codigo.toUpperCase(Locale.ROOT)), solicitante, rol);
  }

  public List<Comentario> comentarios(
      OrganizacionId organizacionId, TicketId ticketId, UsuarioId solicitante, Rol rol) {
    return ejecutar(organizacionId, ticketId, solicitante, rol).visiblesPara(rol);
  }

  private static Ticket visible(Optional<Ticket> ticket, UsuarioId solicitante, Rol rol) {
    return ticket.filter(encontrado -> encontrado.visiblePara(solicitante, rol)).orElseThrow(NoEncontrado::new);
  }
}
