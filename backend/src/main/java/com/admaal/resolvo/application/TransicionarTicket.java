package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.EventoTicket;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.TipoEvento;

public final class TransicionarTicket {

  private final TicketRepositorio tickets;
  private final PublicadorEventos eventos;
  private final Reloj reloj;

  public TransicionarTicket(TicketRepositorio tickets, PublicadorEventos eventos, Reloj reloj) {
    this.tickets = tickets;
    this.eventos = eventos;
    this.reloj = reloj;
  }

  public Ticket ejecutar(OrganizacionId organizacionId, TicketId ticketId, EstadoTicket destino, Rol rol) {
    Ticket ticket = tickets.porId(organizacionId, ticketId).orElseThrow(NoEncontrado::new);
    EstadoTicket origen = ticket.estado();
    ticket.transicionar(destino, rol);
    tickets.guardar(ticket);
    publicar(ticket, origen, destino);
    return ticket;
  }

  /** El evento se escribe en la misma transaccion que el ticket: ni evento sin cambio ni cambio sin evento. */
  private void publicar(Ticket ticket, EstadoTicket origen, EstadoTicket destino) {
    TipoEvento tipo = null;
    if (destino == EstadoTicket.RESUELTO || destino == EstadoTicket.CERRADO) {
      tipo = TipoEvento.TICKET_RESUELTO;
    } else if (origen == EstadoTicket.RESUELTO || origen == EstadoTicket.CERRADO) {
      tipo = TipoEvento.TICKET_REABIERTO;
    }
    if (tipo != null) {
      eventos.publicar(EventoTicket.nuevo(tipo, ticket.organizacionId(), ticket.id(), reloj.ahora()));
    }
  }
}
