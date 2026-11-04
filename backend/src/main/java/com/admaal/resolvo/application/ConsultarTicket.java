package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Comentario;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import java.util.List;

public final class ConsultarTicket {

  private final TicketRepositorio tickets;

  public ConsultarTicket(TicketRepositorio tickets) {
    this.tickets = tickets;
  }

  public Ticket ejecutar(OrganizacionId organizacionId, TicketId ticketId) {
    return tickets.porId(organizacionId, ticketId).orElseThrow(NoEncontrado::new);
  }

  public List<Comentario> comentarios(OrganizacionId organizacionId, TicketId ticketId, Rol rol) {
    return ejecutar(organizacionId, ticketId).visiblesPara(rol);
  }
}
