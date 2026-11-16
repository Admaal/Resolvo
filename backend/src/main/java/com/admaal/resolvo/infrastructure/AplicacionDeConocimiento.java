package com.admaal.resolvo.infrastructure;

import com.admaal.resolvo.application.Articulos;
import com.admaal.resolvo.application.BaseConocimiento;
import com.admaal.resolvo.application.DescartarSugerencia;
import com.admaal.resolvo.application.AprobarSugerencia;
import com.admaal.resolvo.application.Embebedor;
import com.admaal.resolvo.application.IndexarConocimiento;
import com.admaal.resolvo.application.RedactorRespuestas;
import com.admaal.resolvo.application.Reloj;
import com.admaal.resolvo.application.SugerirRespuesta;
import com.admaal.resolvo.application.TicketRepositorio;
import com.admaal.resolvo.domain.Articulo;
import com.admaal.resolvo.domain.Comentario;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Sugerencia;
import com.admaal.resolvo.domain.SugerenciaId;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.UsuarioId;
import com.admaal.resolvo.infrastructure.persistencia.CitaDetalle;
import com.admaal.resolvo.infrastructure.persistencia.SugerenciasJdbc;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de la fase 3. Sugerir e indexar llaman al proveedor de IA y no abren transaccion:
 * no se mantiene una conexion a la base de datos esperando a un servicio externo.
 */
@Service
public class AplicacionDeConocimiento {

  private static final Logger LOG = LoggerFactory.getLogger(AplicacionDeConocimiento.class);

  public record SugerenciaVista(Sugerencia sugerencia, List<CitaDetalle> citas, boolean creada) {}

  private final Reloj reloj;
  private final TicketRepositorio tickets;
  private final SugerenciasJdbc sugerencias;
  private final Articulos articulos;
  private final Embebedor embebedor;
  private final BaseConocimiento base;
  private final RedactorRespuestas redactor;
  private final Executor indexador;

  public AplicacionDeConocimiento(
      Reloj reloj,
      TicketRepositorio tickets,
      SugerenciasJdbc sugerencias,
      Articulos articulos,
      Embebedor embebedor,
      BaseConocimiento base,
      RedactorRespuestas redactor,
      @Qualifier("indexadorEjecutor") Executor indexador) {
    this.reloj = reloj;
    this.tickets = tickets;
    this.sugerencias = sugerencias;
    this.articulos = articulos;
    this.embebedor = embebedor;
    this.base = base;
    this.redactor = redactor;
    this.indexador = indexador;
  }

  public SugerenciaVista sugerir(
      OrganizacionId organizacionId, TicketId ticketId, UsuarioId actor, Rol rol) {
    boolean existia = sugerencias.pendienteDe(organizacionId, ticketId).isPresent();
    try {
      Sugerencia sugerencia =
          new SugerirRespuesta(tickets, sugerencias, embebedor, base, redactor, reloj)
              .ejecutar(organizacionId, ticketId, actor, rol);
      return vista(sugerencia, !existia);
    } catch (DuplicateKeyException carrera) {
      // Dos peticiones a la vez: gana la primera y la segunda devuelve su sugerencia.
      return vista(sugerencias.pendienteDe(organizacionId, ticketId).orElseThrow(), false);
    }
  }

  @Transactional(readOnly = true)
  public SugerenciaVista actual(OrganizacionId organizacionId, TicketId ticketId, Rol rol) {
    exigirPersonal(rol);
    return vista(
        sugerencias.pendienteDe(organizacionId, ticketId).orElseThrow(NoEncontrado::new), false);
  }

  @Transactional
  public Comentario aprobar(
      OrganizacionId organizacionId,
      SugerenciaId sugerenciaId,
      UsuarioId actor,
      Rol rol,
      String texto) {
    return new AprobarSugerencia(sugerencias, tickets, reloj)
        .ejecutar(organizacionId, sugerenciaId, actor, rol, texto);
  }

  @Transactional
  public SugerenciaVista descartar(
      OrganizacionId organizacionId, SugerenciaId sugerenciaId, UsuarioId actor, Rol rol) {
    new DescartarSugerencia(sugerencias, reloj).ejecutar(organizacionId, sugerenciaId, actor, rol);
    return vista(sugerencias.porId(organizacionId, sugerenciaId).orElseThrow(), false);
  }

  @Transactional(readOnly = true)
  public Articulo articulo(OrganizacionId organizacionId, UUID id, Rol rol) {
    exigirPersonal(rol);
    return articulos.porId(organizacionId, id).orElseThrow(NoEncontrado::new);
  }

  public void reindexar(OrganizacionId organizacionId, Rol rol) {
    if (rol != Rol.ADMIN) {
      throw new Prohibido();
    }
    indexar().organizacion(organizacionId);
  }

  /** Indexa un ticket recien resuelto sin retrasar la respuesta ni romperla si falla. */
  public void indexarTicketEnSegundoPlano(OrganizacionId organizacionId, TicketId ticketId) {
    indexador.execute(
        () -> {
          try {
            tickets.porId(organizacionId, ticketId).ifPresent(indexar()::ticket);
          } catch (RuntimeException error) {
            LOG.warn("No se pudo indexar el ticket {}: {}", ticketId.valor(), error.getMessage());
          }
        });
  }

  IndexarConocimiento indexar() {
    return new IndexarConocimiento(embebedor, base, articulos, tickets);
  }

  private SugerenciaVista vista(Sugerencia sugerencia, boolean creada) {
    return new SugerenciaVista(sugerencia, sugerencias.citasDe(sugerencia.id()), creada);
  }

  private static void exigirPersonal(Rol rol) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
  }
}
