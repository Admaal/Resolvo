package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.domain.EstadoSugerencia;
import com.admaal.resolvo.domain.FuenteConocimiento;
import com.admaal.resolvo.domain.SugerenciaId;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.infrastructure.AplicacionDeConocimiento;
import com.admaal.resolvo.infrastructure.AplicacionDeConocimiento.SugerenciaVista;
import com.admaal.resolvo.infrastructure.persistencia.CitaDetalle;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SugerenciaControlador {

  private final AplicacionDeConocimiento conocimiento;

  public SugerenciaControlador(AplicacionDeConocimiento conocimiento) {
    this.conocimiento = conocimiento;
  }

  @PostMapping("/api/tickets/{id}/sugerencias")
  ResponseEntity<SugerenciaRespuesta> sugerir(@PathVariable UUID id) {
    Actor actor = Actor.actual();
    SugerenciaVista vista =
        conocimiento.sugerir(actor.organizacionId(), new TicketId(id), actor.id(), actor.rol());
    return ResponseEntity.status(vista.creada() ? 201 : 200).body(SugerenciaRespuesta.de(vista));
  }

  @GetMapping("/api/tickets/{id}/sugerencias/actual")
  SugerenciaRespuesta actual(@PathVariable UUID id) {
    Actor actor = Actor.actual();
    return SugerenciaRespuesta.de(conocimiento.actual(actor.organizacionId(), new TicketId(id), actor.rol()));
  }

  @PostMapping("/api/sugerencias/{id}/aprobar")
  TicketControlador.ComentarioRespuesta aprobar(@PathVariable UUID id, @RequestBody Aprobacion cuerpo) {
    Actor actor = Actor.actual();
    return TicketControlador.ComentarioRespuesta.de(
        conocimiento.aprobar(
            actor.organizacionId(), new SugerenciaId(id), actor.id(), actor.rol(), cuerpo.texto()));
  }

  @PostMapping("/api/sugerencias/{id}/descartar")
  SugerenciaRespuesta descartar(@PathVariable UUID id) {
    Actor actor = Actor.actual();
    return SugerenciaRespuesta.de(
        conocimiento.descartar(actor.organizacionId(), new SugerenciaId(id), actor.id(), actor.rol()));
  }

  public record Aprobacion(String texto) {}

  public record CitaRespuesta(int orden, FuenteConocimiento fuente, UUID fuenteId, String titulo) {
    static CitaRespuesta de(CitaDetalle cita) {
      return new CitaRespuesta(cita.orden(), cita.fuente(), cita.fuenteId(), cita.titulo());
    }
  }

  public record SugerenciaRespuesta(
      UUID id,
      UUID ticketId,
      String borrador,
      EstadoSugerencia estado,
      String modelo,
      Instant creadaEn,
      List<CitaRespuesta> citas) {
    static SugerenciaRespuesta de(SugerenciaVista vista) {
      return new SugerenciaRespuesta(
          vista.sugerencia().id().valor(),
          vista.sugerencia().ticketId().valor(),
          vista.sugerencia().borrador(),
          vista.sugerencia().estado(),
          vista.sugerencia().modelo(),
          vista.sugerencia().creadaEn(),
          vista.citas().stream().map(CitaRespuesta::de).toList());
    }
  }
}
