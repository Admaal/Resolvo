package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.FragmentoId;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.SinFuentes;
import com.admaal.resolvo.domain.Sugerencia;
import com.admaal.resolvo.domain.SugerenciaId;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.UsuarioId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SugerirRespuesta {

  static final int FRAGMENTOS = 5;

  private final TicketRepositorio tickets;
  private final Sugerencias sugerencias;
  private final Embebedor embebedor;
  private final BaseConocimiento base;
  private final RedactorRespuestas redactor;
  private final Reloj reloj;
  private final double distanciaMaxima;
  private final Presupuesto presupuesto;

  public SugerirRespuesta(
      TicketRepositorio tickets,
      Sugerencias sugerencias,
      Embebedor embebedor,
      BaseConocimiento base,
      RedactorRespuestas redactor,
      Reloj reloj) {
    this(tickets, sugerencias, embebedor, base, redactor, reloj, BaseConocimiento.SIN_LIMITE);
  }

  /** Con {@code distanciaMaxima}, un fragmento poco parecido al ticket no se cita aunque sea el mas cercano. */
  public SugerirRespuesta(
      TicketRepositorio tickets,
      Sugerencias sugerencias,
      Embebedor embebedor,
      BaseConocimiento base,
      RedactorRespuestas redactor,
      Reloj reloj,
      double distanciaMaxima) {
    this(tickets, sugerencias, embebedor, base, redactor, reloj, distanciaMaxima, Presupuesto.SIN_LIMITE);
  }

  /** Cada sugerencia nueva reserva una llamada del presupuesto diario de la organizacion. */
  public SugerirRespuesta(
      TicketRepositorio tickets,
      Sugerencias sugerencias,
      Embebedor embebedor,
      BaseConocimiento base,
      RedactorRespuestas redactor,
      Reloj reloj,
      double distanciaMaxima,
      Presupuesto presupuesto) {
    this.tickets = tickets;
    this.sugerencias = sugerencias;
    this.embebedor = embebedor;
    this.base = base;
    this.redactor = redactor;
    this.reloj = reloj;
    this.distanciaMaxima = distanciaMaxima;
    this.presupuesto = presupuesto;
  }

  public Sugerencia ejecutar(
      OrganizacionId organizacionId, TicketId ticketId, UsuarioId actor, Rol rol) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    Ticket ticket = tickets.porId(organizacionId, ticketId).orElseThrow(NoEncontrado::new);
    Optional<Sugerencia> existente = sugerencias.pendienteDe(organizacionId, ticketId);
    if (existente.isPresent()) {
      return existente.get();
    }
    try (Presupuesto.Reserva reserva = presupuesto.reservar(organizacionId)) {
      List<Fragmento> fragmentos =
          base.buscar(
              organizacionId,
              embebedor.embeber(ticket.asunto() + "\n" + ticket.cuerpo()),
              FRAGMENTOS,
              ticket.id().valor(),
              distanciaMaxima);
      if (fragmentos.isEmpty()) {
        throw new SinFuentes();
      }
      BorradorConCitas borrador = redactor.redactar(ticket, fragmentos);
      Set<FragmentoId> recuperados = new HashSet<>();
      for (Fragmento fragmento : fragmentos) {
        recuperados.add(fragmento.id());
      }
      Sugerencia sugerencia =
          Sugerencia.crear(
              SugerenciaId.nuevo(),
              organizacionId,
              ticketId,
              borrador.texto(),
              redactor.modelo(),
              reloj.ahora(),
              borrador.citas(),
              recuperados);
      sugerencias.guardar(sugerencia);
      return sugerencia;
    }
  }
}
