package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.application.AbrirTicketComando;
import com.admaal.resolvo.application.ConsultaTickets;
import com.admaal.resolvo.application.Pagina;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.Comentario;
import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.UsuarioId;
import com.admaal.resolvo.infrastructure.AplicacionDeConocimiento;
import com.admaal.resolvo.infrastructure.AplicacionDeResolvo;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketControlador {

  private final AplicacionDeResolvo aplicacion;
  private final AplicacionDeConocimiento conocimiento;

  public TicketControlador(AplicacionDeResolvo aplicacion, AplicacionDeConocimiento conocimiento) {
    this.aplicacion = aplicacion;
    this.conocimiento = conocimiento;
  }

  @PostMapping
  TicketRespuesta abrir(@RequestBody Alta cuerpo) {
    Actor actor = Actor.actual();
    Ticket ticket =
        aplicacion.abrir(
            new AbrirTicketComando(
                actor.organizacionId(),
                actor.id(),
                actor.rol(),
                new CategoriaId(cuerpo.categoriaId()),
                cuerpo.prioridad(),
                cuerpo.asunto(),
                cuerpo.cuerpo()));
    return TicketRespuesta.de(ticket, List.of());
  }

  @GetMapping
  PaginaRespuesta listar(
      @RequestParam(required = false) EstadoTicket estado,
      @RequestParam(required = false) Prioridad prioridad,
      @RequestParam(required = false) UUID asignado,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamano) {
    Actor actor = Actor.actual();
    Pagina<Ticket> resultado =
        aplicacion.listar(
            new ConsultaTickets(
                actor.organizacionId(),
                estado,
                prioridad,
                asignado == null ? null : new UsuarioId(asignado),
                pagina,
                tamano));
    return new PaginaRespuesta(
        resultado.elementos().stream().map(TicketResumen::de).toList(),
        resultado.pagina(),
        resultado.tamano(),
        resultado.total());
  }

  @GetMapping("/{id}")
  TicketRespuesta detalle(@PathVariable UUID id) {
    Actor actor = Actor.actual();
    TicketId ticketId = new TicketId(id);
    Ticket ticket = aplicacion.consultar(actor.organizacionId(), ticketId);
    return TicketRespuesta.de(ticket, aplicacion.comentarios(actor.organizacionId(), ticketId, actor.rol()));
  }

  @PostMapping("/{id}/transicion")
  TicketRespuesta transicionar(@PathVariable UUID id, @RequestBody Transicion cuerpo) {
    Actor actor = Actor.actual();
    Ticket ticket =
        aplicacion.transicionar(actor.organizacionId(), new TicketId(id), cuerpo.estado(), actor.rol());
    return TicketRespuesta.de(ticket, aplicacion.comentarios(actor.organizacionId(), ticket.id(), actor.rol()));
  }

  @PostMapping("/{id}/asignacion")
  TicketRespuesta asignar(@PathVariable UUID id, @RequestBody Asignacion cuerpo) {
    Actor actor = Actor.actual();
    Ticket ticket =
        aplicacion.asignar(
            actor.organizacionId(), new TicketId(id), new UsuarioId(cuerpo.asignadoId()), actor.rol());
    return TicketRespuesta.de(ticket, List.of());
  }

  @PostMapping("/{id}/comentarios")
  ComentarioRespuesta comentar(@PathVariable UUID id, @RequestBody ComentarioAlta cuerpo) {
    Actor actor = Actor.actual();
    Comentario comentario =
        aplicacion.comentar(
            actor.organizacionId(),
            new TicketId(id),
            actor.id(),
            actor.rol(),
            cuerpo.cuerpo(),
            cuerpo.interno());
    return ComentarioRespuesta.de(comentario);
  }

  public record Alta(UUID categoriaId, Prioridad prioridad, String asunto, String cuerpo) {}

  public record Transicion(EstadoTicket estado) {}

  public record Asignacion(UUID asignadoId) {}

  public record ComentarioAlta(String cuerpo, boolean interno) {}

  public record ComentarioRespuesta(
      UUID id, UUID autorId, String cuerpo, boolean interno, Instant creadoEn) {
    static ComentarioRespuesta de(Comentario comentario) {
      return new ComentarioRespuesta(
          comentario.id().valor(),
          comentario.autorId().valor(),
          comentario.cuerpo(),
          comentario.interno(),
          comentario.creadoEn());
    }
  }

  public record TicketResumen(
      UUID id,
      String codigo,
      String asunto,
      EstadoTicket estado,
      Prioridad prioridad,
      UUID asignadoId,
      UUID autorId,
      Instant abiertoEn,
      Instant venceEl) {
    static TicketResumen de(Ticket ticket) {
      return new TicketResumen(
          ticket.id().valor(),
          ticket.codigo(),
          ticket.asunto(),
          ticket.estado(),
          ticket.prioridad(),
          ticket.asignadoId() == null ? null : ticket.asignadoId().valor(),
          ticket.autorId().valor(),
          ticket.abiertoEn(),
          ticket.venceEl());
    }
  }

  public record TicketRespuesta(
      UUID id,
      String codigo,
      String asunto,
      String cuerpo,
      EstadoTicket estado,
      Prioridad prioridad,
      UUID categoriaId,
      UUID autorId,
      UUID asignadoId,
      Instant abiertoEn,
      Instant venceEl,
      List<ComentarioRespuesta> comentarios) {
    static TicketRespuesta de(Ticket ticket, List<Comentario> comentarios) {
      return new TicketRespuesta(
          ticket.id().valor(),
          ticket.codigo(),
          ticket.asunto(),
          ticket.cuerpo(),
          ticket.estado(),
          ticket.prioridad(),
          ticket.categoriaId().valor(),
          ticket.autorId().valor(),
          ticket.asignadoId() == null ? null : ticket.asignadoId().valor(),
          ticket.abiertoEn(),
          ticket.venceEl(),
          comentarios.stream().map(ComentarioRespuesta::de).toList());
    }
  }

  public record PaginaRespuesta(List<TicketResumen> elementos, int pagina, int tamano, long total) {}
}
