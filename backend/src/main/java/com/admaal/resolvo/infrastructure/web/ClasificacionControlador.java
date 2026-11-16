package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.Clasificacion;
import com.admaal.resolvo.domain.ClasificacionId;
import com.admaal.resolvo.domain.EstadoClasificacion;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.infrastructure.AplicacionDeClasificacion;
import com.admaal.resolvo.infrastructure.AplicacionDeClasificacion.ClasificacionVista;
import com.admaal.resolvo.infrastructure.AplicacionDeClasificacion.TicketClasificado;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClasificacionControlador {

  private final AplicacionDeClasificacion aplicacion;

  public ClasificacionControlador(AplicacionDeClasificacion aplicacion) {
    this.aplicacion = aplicacion;
  }

  @PostMapping("/api/tickets/{id}/clasificaciones")
  ResponseEntity<ClasificacionRespuesta> sugerir(@PathVariable UUID id) {
    Actor actor = Actor.actual();
    ClasificacionVista vista =
        aplicacion.sugerir(actor.organizacionId(), new TicketId(id), actor.id(), actor.rol());
    return ResponseEntity.status(vista.creada() ? 201 : 200)
        .body(ClasificacionRespuesta.de(vista.clasificacion()));
  }

  @GetMapping("/api/tickets/{id}/clasificaciones/actual")
  ClasificacionRespuesta actual(@PathVariable UUID id) {
    Actor actor = Actor.actual();
    return ClasificacionRespuesta.de(
        aplicacion.actual(actor.organizacionId(), new TicketId(id), actor.rol()));
  }

  @PostMapping("/api/clasificaciones/{id}/aplicacion")
  TicketControlador.TicketRespuesta aplicar(@PathVariable UUID id, @RequestBody Aplicacion cuerpo) {
    Actor actor = Actor.actual();
    TicketClasificado resultado =
        aplicacion.aplicar(
            actor.organizacionId(),
            new ClasificacionId(id),
            new CategoriaId(cuerpo.categoriaId()),
            cuerpo.prioridad(),
            actor.id(),
            actor.rol());
    return TicketControlador.TicketRespuesta.de(resultado.ticket(), resultado.comentarios());
  }

  @PostMapping("/api/clasificaciones/{id}/descarte")
  ClasificacionRespuesta descartar(@PathVariable UUID id) {
    Actor actor = Actor.actual();
    return ClasificacionRespuesta.de(
        aplicacion.descartar(actor.organizacionId(), new ClasificacionId(id), actor.id(), actor.rol()));
  }

  public record Aplicacion(UUID categoriaId, Prioridad prioridad) {}

  public record ClasificacionRespuesta(
      UUID id,
      UUID ticketId,
      UUID categoriaId,
      Prioridad prioridad,
      String motivo,
      EstadoClasificacion estado,
      String modelo,
      Instant creadaEn) {
    static ClasificacionRespuesta de(Clasificacion clasificacion) {
      return new ClasificacionRespuesta(
          clasificacion.id().valor(),
          clasificacion.ticketId().valor(),
          clasificacion.categoriaId().valor(),
          clasificacion.prioridad(),
          clasificacion.motivo(),
          clasificacion.estado(),
          clasificacion.modelo(),
          clasificacion.creadaEn());
    }
  }
}
