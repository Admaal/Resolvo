package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.EventoTicket;
import com.admaal.resolvo.domain.TipoEvento;

public final class IndexarTicketResuelto implements ConsumidorEventos {

  private final IndexarConocimiento indexar;
  private final TicketRepositorio tickets;

  public IndexarTicketResuelto(IndexarConocimiento indexar, TicketRepositorio tickets) {
    this.indexar = indexar;
    this.tickets = tickets;
  }

  @Override
  public String nombre() {
    return "indexar-ticket";
  }

  @Override
  public boolean admite(TipoEvento tipo) {
    return tipo == TipoEvento.TICKET_RESUELTO;
  }

  /** Lee el ticket tal como esta ahora: si ya no esta resuelto, {@code indexar.ticket} lo ignora. */
  @Override
  public void consumir(EventoTicket evento) {
    tickets.porId(evento.organizacionId(), evento.ticketId()).ifPresent(indexar::ticket);
  }
}
