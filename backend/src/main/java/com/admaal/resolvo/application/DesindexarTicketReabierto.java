package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.EventoTicket;
import com.admaal.resolvo.domain.FuenteConocimiento;
import com.admaal.resolvo.domain.TipoEvento;

public final class DesindexarTicketReabierto implements ConsumidorEventos {

  private final BaseConocimiento base;
  private final TicketRepositorio tickets;

  public DesindexarTicketReabierto(BaseConocimiento base, TicketRepositorio tickets) {
    this.base = base;
    this.tickets = tickets;
  }

  @Override
  public String nombre() {
    return "desindexar-ticket";
  }

  @Override
  public boolean admite(TipoEvento tipo) {
    return tipo == TipoEvento.TICKET_REABIERTO;
  }

  /**
   * Lee el ticket tal como esta ahora, igual que el indexado: si un reintento llega cuando el
   * ticket ya se volvio a resolver, sus fragmentos son validos y no se borran.
   */
  @Override
  public void consumir(EventoTicket evento) {
    boolean resueltoOtraVez =
        tickets
            .porId(evento.organizacionId(), evento.ticketId())
            .filter(
                ticket ->
                    ticket.estado() == EstadoTicket.RESUELTO
                        || ticket.estado() == EstadoTicket.CERRADO)
            .isPresent();
    if (!resueltoOtraVez) {
      base.borrar(evento.organizacionId(), FuenteConocimiento.TICKET, evento.ticketId().valor());
    }
  }
}
