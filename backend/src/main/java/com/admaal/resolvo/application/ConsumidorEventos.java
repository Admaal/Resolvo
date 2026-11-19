package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.EventoTicket;
import com.admaal.resolvo.domain.TipoEvento;

public interface ConsumidorEventos {

  /** Nombre estable: forma parte de la clave de idempotencia. */
  String nombre();

  boolean admite(TipoEvento tipo);

  void consumir(EventoTicket evento);
}
