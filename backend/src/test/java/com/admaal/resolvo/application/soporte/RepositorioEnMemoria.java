package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.TicketRepositorio;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RepositorioEnMemoria implements TicketRepositorio {

  private final ArrayList<Ticket> guardados = new ArrayList<>();
  private int vecesGuardado;

  @Override
  public void guardar(Ticket ticket) {
    vecesGuardado++;
    guardados.removeIf(existente -> existente.id().equals(ticket.id()));
    guardados.add(ticket);
  }

  @Override
  public Optional<Ticket> porId(OrganizacionId organizacionId, TicketId ticketId) {
    return guardados.stream()
        .filter(ticket -> ticket.id().equals(ticketId) && ticket.organizacionId().equals(organizacionId))
        .findFirst();
  }

  @Override
  public List<Ticket> deLaOrganizacion(OrganizacionId organizacionId) {
    return guardados.stream().filter(ticket -> ticket.organizacionId().equals(organizacionId)).toList();
  }

  public int vecesGuardado() {
    return vecesGuardado;
  }
}
