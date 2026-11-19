package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.EventoTicket;

/** Deja el evento en la bandeja de salida; el adapter lo escribe en la transaccion en curso. */
public interface PublicadorEventos {
  void publicar(EventoTicket evento);
}
