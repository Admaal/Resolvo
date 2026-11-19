package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.EventoTicket;
import java.util.List;
import java.util.UUID;

/** Lado del procesador de la bandeja de salida (outbox). */
public interface BandejaEventos {

  /** Un evento reclamado y cuantas veces se ha reclamado ya, contando esta. */
  record Reclamado(EventoTicket evento, int intentos) {}

  /**
   * Reclama hasta {@code maximo} pendientes, por orden de aparicion. Nadie mas los recibe mientras
   * dure el reclamo.
   */
  List<Reclamado> reclamar(int maximo);

  void marcarHecho(UUID eventoId);

  /** Anota el fallo. Si es definitivo el evento deja de reintentarse; si no, vuelve al vencer el reclamo. */
  void fallar(UUID eventoId, String error, boolean definitivo);
}
