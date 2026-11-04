package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.PoliticaSla;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import java.time.Instant;

public final class AbrirTicket {

  private final Reloj reloj;
  private final CodigosDeTicket codigos;
  private final TicketRepositorio tickets;
  private final Categorias categorias;
  private final PoliticaSla sla = new PoliticaSla();

  public AbrirTicket(
      Reloj reloj, CodigosDeTicket codigos, TicketRepositorio tickets, Categorias categorias) {
    this.reloj = reloj;
    this.codigos = codigos;
    this.tickets = tickets;
    this.categorias = categorias;
  }

  public Ticket ejecutar(AbrirTicketComando comando) {
    Categoria categoria =
        categorias
            .porId(comando.organizacionId(), comando.categoriaId())
            .orElseThrow(NoEncontrado::new);
    Instant ahora = reloj.ahora();
    Ticket ticket =
        Ticket.abrir(
            TicketId.nuevo(),
            comando.organizacionId(),
            comando.autorId(),
            categoria.id(),
            comando.prioridad(),
            codigos.siguiente(comando.organizacionId()),
            comando.asunto(),
            comando.cuerpo(),
            ahora,
            ahora.plus(sla.primeraRespuesta(categoria.nombre(), comando.prioridad())));
    tickets.guardar(ticket);
    return ticket;
  }
}
