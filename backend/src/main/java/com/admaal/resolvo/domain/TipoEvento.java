package com.admaal.resolvo.domain;

/** Solo los eventos que hoy tienen un consumidor (ADR-0008). */
public enum TipoEvento {
  TICKET_RESUELTO,
  TICKET_REABIERTO
}
