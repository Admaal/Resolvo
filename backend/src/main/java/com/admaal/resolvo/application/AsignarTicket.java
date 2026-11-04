package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.Usuario;
import com.admaal.resolvo.domain.UsuarioId;

public final class AsignarTicket {

  private final TicketRepositorio tickets;
  private final Usuarios usuarios;

  public AsignarTicket(TicketRepositorio tickets, Usuarios usuarios) {
    this.tickets = tickets;
    this.usuarios = usuarios;
  }

  public Ticket ejecutar(
      OrganizacionId organizacionId, TicketId ticketId, UsuarioId asignadoId, Rol actor) {
    Ticket ticket = tickets.porId(organizacionId, ticketId).orElseThrow(NoEncontrado::new);
    if (actor != Rol.CLIENTE) {
      Usuario destino = usuarios.porId(asignadoId).orElseThrow(NoEncontrado::new);
      if (!destino.organizacionId().equals(organizacionId)
          || (destino.rol() != Rol.AGENTE && destino.rol() != Rol.ADMIN)) {
        throw new NoEncontrado();
      }
    }
    ticket.asignar(asignadoId, actor);
    tickets.guardar(ticket);
    return ticket;
  }
}
