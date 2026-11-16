package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.Clasificacion;
import com.admaal.resolvo.domain.ClasificacionId;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.PoliticaSla;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.UsuarioId;

/** Aplica al ticket lo que la persona elige, que puede diferir de lo que propuso el modelo. */
public final class AplicarClasificacion {

  private final Clasificaciones clasificaciones;
  private final TicketRepositorio tickets;
  private final Categorias categorias;
  private final Reloj reloj;
  private final PoliticaSla sla = new PoliticaSla();

  public AplicarClasificacion(
      Clasificaciones clasificaciones,
      TicketRepositorio tickets,
      Categorias categorias,
      Reloj reloj) {
    this.clasificaciones = clasificaciones;
    this.tickets = tickets;
    this.categorias = categorias;
    this.reloj = reloj;
  }

  public Ticket ejecutar(
      OrganizacionId organizacionId,
      ClasificacionId clasificacionId,
      CategoriaId categoriaId,
      Prioridad prioridad,
      UsuarioId actor,
      Rol rol) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    Clasificacion clasificacion =
        clasificaciones.porId(organizacionId, clasificacionId).orElseThrow(NoEncontrado::new);
    Ticket ticket =
        tickets.porId(organizacionId, clasificacion.ticketId()).orElseThrow(NoEncontrado::new);
    Categoria categoria =
        categorias.porId(organizacionId, categoriaId).orElseThrow(NoEncontrado::new);
    ticket.reclasificar(categoria, prioridad, rol, sla);
    clasificacion.aplicar(actor, rol, reloj.ahora());
    tickets.guardar(ticket);
    clasificaciones.guardar(clasificacion);
    return ticket;
  }
}
