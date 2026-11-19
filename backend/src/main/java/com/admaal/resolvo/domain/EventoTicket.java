package com.admaal.resolvo.domain;

import java.time.Instant;
import java.util.UUID;

/** Algo que ocurrio en un ticket y que otro proceso debe atender. El id es la clave de idempotencia. */
public record EventoTicket(
    UUID id, TipoEvento tipo, OrganizacionId organizacionId, TicketId ticketId, Instant ocurridoEn) {

  public static EventoTicket nuevo(
      TipoEvento tipo, OrganizacionId organizacionId, TicketId ticketId, Instant ocurridoEn) {
    return new EventoTicket(UUID.randomUUID(), tipo, organizacionId, ticketId, ocurridoEn);
  }
}
