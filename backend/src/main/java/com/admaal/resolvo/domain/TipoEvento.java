package com.admaal.resolvo.domain;

/** Solo los eventos que hoy tienen un consumidor (ADR-0008). */
public enum TipoEvento {
  TICKET_RESUELTO,
  TICKET_REABIERTO,
  /** Se oculto o restauro un comentario de un ticket ya resuelto: el indice del RAG debe actualizarse. */
  COMENTARIO_MODERADO
}
