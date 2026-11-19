package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.EventoTicket;
import com.admaal.resolvo.domain.FuenteConocimiento;
import com.admaal.resolvo.domain.TipoEvento;

public final class DesindexarTicketReabierto implements ConsumidorEventos {

  private final BaseConocimiento base;

  public DesindexarTicketReabierto(BaseConocimiento base) {
    this.base = base;
  }

  @Override
  public String nombre() {
    return "desindexar-ticket";
  }

  @Override
  public boolean admite(TipoEvento tipo) {
    return tipo == TipoEvento.TICKET_REABIERTO;
  }

  @Override
  public void consumir(EventoTicket evento) {
    base.borrar(evento.organizacionId(), FuenteConocimiento.TICKET, evento.ticketId().valor());
  }
}
