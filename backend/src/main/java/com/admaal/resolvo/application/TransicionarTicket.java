package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;

public final class TransicionarTicket {

  private final TicketRepositorio tickets;

  public TransicionarTicket(TicketRepositorio tickets) {
    this.tickets = tickets;
  }

  public Ticket ejecutar(OrganizacionId organizacionId, TicketId ticketId, EstadoTicket destino, Rol rol) {
    Ticket ticket = tickets.porId(organizacionId, ticketId).orElseThrow(NoEncontrado::new);
    ticket.transicionar(destino, rol);
    tickets.guardar(ticket);
    return ticket;
  }
}
