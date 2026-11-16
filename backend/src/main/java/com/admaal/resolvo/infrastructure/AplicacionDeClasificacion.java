package com.admaal.resolvo.infrastructure;

import com.admaal.resolvo.application.AplicarClasificacion;
import com.admaal.resolvo.application.CatalogoDeCategorias;
import com.admaal.resolvo.application.Categorias;
import com.admaal.resolvo.application.Clasificaciones;
import com.admaal.resolvo.application.ClasificadorTickets;
import com.admaal.resolvo.application.DescartarClasificacion;
import com.admaal.resolvo.application.Reloj;
import com.admaal.resolvo.application.SugerirClasificacion;
import com.admaal.resolvo.application.TicketRepositorio;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.Clasificacion;
import com.admaal.resolvo.domain.ClasificacionId;
import com.admaal.resolvo.domain.Comentario;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.UsuarioId;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de la fase 4. Sugerir llama al proveedor de IA y no abre transaccion: no se
 * mantiene una conexion a la base de datos esperando a un servicio externo.
 */
@Service
public class AplicacionDeClasificacion {

  public record ClasificacionVista(Clasificacion clasificacion, boolean creada) {}

  public record TicketClasificado(Ticket ticket, List<Comentario> comentarios) {}

  private final Reloj reloj;
  private final TicketRepositorio tickets;
  private final Clasificaciones clasificaciones;
  private final CatalogoDeCategorias catalogo;
  private final Categorias categorias;
  private final ClasificadorTickets clasificador;

  public AplicacionDeClasificacion(
      Reloj reloj,
      TicketRepositorio tickets,
      Clasificaciones clasificaciones,
      CatalogoDeCategorias catalogo,
      Categorias categorias,
      ClasificadorTickets clasificador) {
    this.reloj = reloj;
    this.tickets = tickets;
    this.clasificaciones = clasificaciones;
    this.catalogo = catalogo;
    this.categorias = categorias;
    this.clasificador = clasificador;
  }

  public ClasificacionVista sugerir(
      OrganizacionId organizacionId, TicketId ticketId, UsuarioId actor, Rol rol) {
    boolean existia = clasificaciones.pendienteDe(organizacionId, ticketId).isPresent();
    try {
      Clasificacion clasificacion =
          new SugerirClasificacion(tickets, clasificaciones, catalogo, clasificador, reloj)
              .ejecutar(organizacionId, ticketId, actor, rol);
      return new ClasificacionVista(clasificacion, !existia);
    } catch (DuplicateKeyException carrera) {
      // Dos peticiones a la vez: gana la primera y la segunda devuelve su clasificacion.
      return new ClasificacionVista(
          clasificaciones.pendienteDe(organizacionId, ticketId).orElseThrow(), false);
    }
  }

  @Transactional(readOnly = true)
  public Clasificacion actual(OrganizacionId organizacionId, TicketId ticketId, Rol rol) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    return clasificaciones.pendienteDe(organizacionId, ticketId).orElseThrow(NoEncontrado::new);
  }

  @Transactional
  public TicketClasificado aplicar(
      OrganizacionId organizacionId,
      ClasificacionId clasificacionId,
      CategoriaId categoriaId,
      Prioridad prioridad,
      UsuarioId actor,
      Rol rol) {
    Ticket ticket =
        new AplicarClasificacion(clasificaciones, tickets, categorias, reloj)
            .ejecutar(organizacionId, clasificacionId, categoriaId, prioridad, actor, rol);
    return new TicketClasificado(ticket, ticket.visiblesPara(rol));
  }

  @Transactional
  public Clasificacion descartar(
      OrganizacionId organizacionId, ClasificacionId clasificacionId, UsuarioId actor, Rol rol) {
    new DescartarClasificacion(clasificaciones, reloj)
        .ejecutar(organizacionId, clasificacionId, actor, rol);
    return clasificaciones.porId(organizacionId, clasificacionId).orElseThrow();
  }
}
