package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.Clasificacion;
import com.admaal.resolvo.domain.ClasificacionId;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.TextoInvalido;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.TransicionNoPermitida;
import com.admaal.resolvo.domain.UsuarioId;
import java.util.List;
import java.util.Optional;

public final class SugerirClasificacion {

  private static final String INVALIDA = "clasificacion-invalida";

  private final TicketRepositorio tickets;
  private final Clasificaciones clasificaciones;
  private final CatalogoDeCategorias catalogo;
  private final ClasificadorTickets clasificador;
  private final Reloj reloj;
  private final Presupuesto presupuesto;

  public SugerirClasificacion(
      TicketRepositorio tickets,
      Clasificaciones clasificaciones,
      CatalogoDeCategorias catalogo,
      ClasificadorTickets clasificador,
      Reloj reloj) {
    this(tickets, clasificaciones, catalogo, clasificador, reloj, Presupuesto.SIN_LIMITE);
  }

  /** Cada clasificacion nueva reserva una llamada del presupuesto diario de la organizacion. */
  public SugerirClasificacion(
      TicketRepositorio tickets,
      Clasificaciones clasificaciones,
      CatalogoDeCategorias catalogo,
      ClasificadorTickets clasificador,
      Reloj reloj,
      Presupuesto presupuesto) {
    this.tickets = tickets;
    this.clasificaciones = clasificaciones;
    this.catalogo = catalogo;
    this.clasificador = clasificador;
    this.reloj = reloj;
    this.presupuesto = presupuesto;
  }

  public Clasificacion ejecutar(
      OrganizacionId organizacionId, TicketId ticketId, UsuarioId actor, Rol rol) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    Ticket ticket = tickets.porId(organizacionId, ticketId).orElseThrow(NoEncontrado::new);
    if (!ticket.reclasificable()) {
      throw new TransicionNoPermitida();
    }
    Optional<Clasificacion> existente = clasificaciones.pendienteDe(organizacionId, ticketId);
    if (existente.isPresent()) {
      return existente.get();
    }
    try (Presupuesto.Reserva reserva = presupuesto.reservar(organizacionId)) {
      List<Categoria> categorias = catalogo.deLaOrganizacion(organizacionId);
      ClasificacionPropuesta propuesta = clasificador.clasificar(ticket, categorias);
      if (propuesta.prioridad() == null) {
        throw new IaNoDisponible(INVALIDA);
      }
      Categoria categoria = categoriaNombrada(categorias, propuesta.categoria());
      Clasificacion clasificacion;
      try {
        clasificacion =
            Clasificacion.proponer(
                ClasificacionId.nuevo(),
                organizacionId,
                ticketId,
                categoria.id(),
                propuesta.prioridad(),
                propuesta.motivo(),
                clasificador.modelo(),
                reloj.ahora());
      } catch (TextoInvalido invalido) {
        throw new IaNoDisponible(INVALIDA, invalido);
      }
      clasificaciones.guardar(clasificacion);
      return clasificacion;
    }
  }

  private static Categoria categoriaNombrada(List<Categoria> categorias, String nombre) {
    if (nombre == null) {
      throw new IaNoDisponible(INVALIDA);
    }
    String buscado = nombre.strip();
    return categorias.stream()
        .filter(categoria -> categoria.nombre().equalsIgnoreCase(buscado))
        .findFirst()
        .orElseThrow(() -> new IaNoDisponible(INVALIDA));
  }
}
