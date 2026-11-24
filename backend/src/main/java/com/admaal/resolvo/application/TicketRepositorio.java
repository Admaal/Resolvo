package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import java.util.List;
import java.util.Optional;

public interface TicketRepositorio {
  void guardar(Ticket ticket);

  Optional<Ticket> porId(OrganizacionId organizacionId, TicketId ticketId);

  /** El codigo ({@code RS-3}) es unico por organizacion: otra organizacion no lo ve. */
  Optional<Ticket> porCodigo(OrganizacionId organizacionId, String codigo);

  List<Ticket> deLaOrganizacion(OrganizacionId organizacionId);
}
