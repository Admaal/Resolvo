package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.TicketRepositorio;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TicketRepositorioJpa implements TicketRepositorio {

  private final TicketJpa tickets;

  public TicketRepositorioJpa(TicketJpa tickets) {
    this.tickets = tickets;
  }

  @Override
  public void guardar(Ticket ticket) {
    TicketEntity fila = tickets.findById(ticket.id().valor()).orElseGet(TicketEntity::new);
    Mapeo.copiar(ticket, fila);
    tickets.save(fila);
  }

  @Override
  public Optional<Ticket> porId(OrganizacionId organizacionId, TicketId ticketId) {
    return tickets
        .porIdEnOrganizacion(organizacionId.valor(), ticketId.valor())
        .map(Mapeo::ticket);
  }

  @Override
  public Optional<Ticket> porCodigo(OrganizacionId organizacionId, String codigo) {
    return tickets.porCodigoEnOrganizacion(organizacionId.valor(), codigo).map(Mapeo::ticket);
  }

  @Override
  public List<Ticket> deLaOrganizacion(OrganizacionId organizacionId) {
    return tickets.deLaOrganizacion(organizacionId.valor()).stream().map(Mapeo::ticket).toList();
  }
}
