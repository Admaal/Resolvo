package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Comentario;
import com.admaal.resolvo.domain.ComentarioId;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Sugerencia;
import com.admaal.resolvo.domain.SugerenciaId;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;

public final class AprobarSugerencia {

  private final Sugerencias sugerencias;
  private final TicketRepositorio tickets;
  private final Reloj reloj;

  public AprobarSugerencia(Sugerencias sugerencias, TicketRepositorio tickets, Reloj reloj) {
    this.sugerencias = sugerencias;
    this.tickets = tickets;
    this.reloj = reloj;
  }

  public Comentario ejecutar(
      OrganizacionId organizacionId,
      SugerenciaId sugerenciaId,
      UsuarioId actor,
      Rol rol,
      String textoFinal) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    Sugerencia sugerencia =
        sugerencias.porId(organizacionId, sugerenciaId).orElseThrow(NoEncontrado::new);
    Ticket ticket =
        tickets.porId(organizacionId, sugerencia.ticketId()).orElseThrow(NoEncontrado::new);
    Instant ahora = reloj.ahora();
    String texto = sugerencia.aprobar(actor, rol, textoFinal, ahora);
    Comentario comentario = ticket.comentar(ComentarioId.nuevo(), actor, rol, texto, false, ahora);
    tickets.guardar(ticket);
    sugerencias.guardar(sugerencia);
    return comentario;
  }
}
