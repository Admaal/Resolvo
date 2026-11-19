package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.Banco;
import com.admaal.resolvo.application.soporte.PublicadorEnMemoria;
import com.admaal.resolvo.application.soporte.RepositorioEnMemoria;
import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.EventoTicket;
import com.admaal.resolvo.domain.TipoEvento;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TransicionNoPermitida;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransicionarTicketTest {

  private final Instant ahora = Instant.parse("2026-11-04T10:00:00Z");
  private final OrganizacionId organizacion = new OrganizacionId(UUID.randomUUID());
  private final UsuarioId autor = new UsuarioId(UUID.randomUUID());
  private final PublicadorEnMemoria publicador = new PublicadorEnMemoria();

  private TransicionarTicket caso(RepositorioEnMemoria tickets) {
    return new TransicionarTicket(tickets, publicador, () -> ahora);
  }

  @Test
  void resolverPublicaTicketResuelto() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);

    caso(tickets).ejecutar(organizacion, ticket.id(), EstadoTicket.RESUELTO, Rol.AGENTE);

    assertThat(publicador.publicados())
        .singleElement()
        .satisfies(
            evento -> {
              assertThat(evento.tipo()).isEqualTo(TipoEvento.TICKET_RESUELTO);
              assertThat(evento.organizacionId()).isEqualTo(organizacion);
              assertThat(evento.ticketId()).isEqualTo(ticket.id());
              assertThat(evento.ocurridoEn()).isEqualTo(ahora);
              assertThat(evento.id()).isNotNull();
            });
  }

  @Test
  void cerrarPublicaTicketResueltoParaReindexarConLosUltimosComentarios() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);
    caso(tickets).ejecutar(organizacion, ticket.id(), EstadoTicket.RESUELTO, Rol.AGENTE);
    publicador.vaciar();

    caso(tickets).ejecutar(organizacion, ticket.id(), EstadoTicket.CERRADO, Rol.AGENTE);

    assertThat(publicador.publicados())
        .extracting(EventoTicket::tipo)
        .containsExactly(TipoEvento.TICKET_RESUELTO);
  }

  @Test
  void reabrirUnTicketResueltoPublicaTicketReabierto() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);
    caso(tickets).ejecutar(organizacion, ticket.id(), EstadoTicket.RESUELTO, Rol.AGENTE);
    publicador.vaciar();

    caso(tickets).ejecutar(organizacion, ticket.id(), EstadoTicket.ABIERTO, Rol.AGENTE);

    assertThat(publicador.publicados())
        .singleElement()
        .satisfies(
            evento -> {
              assertThat(evento.tipo()).isEqualTo(TipoEvento.TICKET_REABIERTO);
              assertThat(evento.ticketId()).isEqualTo(ticket.id());
            });
  }

  @Test
  void lasDemasTransicionesNoPublicanNada() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);

    caso(tickets).ejecutar(organizacion, ticket.id(), EstadoTicket.PENDIENTE, Rol.AGENTE);
    caso(tickets).ejecutar(organizacion, ticket.id(), EstadoTicket.ABIERTO, Rol.AGENTE);

    assertThat(publicador.publicados()).isEmpty();
  }

  @Test
  void unaTransicionRechazadaNoPublicaNada() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);

    assertThatThrownBy(
            () -> caso(tickets).ejecutar(organizacion, ticket.id(), EstadoTicket.CERRADO, Rol.AGENTE))
        .isInstanceOf(TransicionNoPermitida.class);
    assertThatThrownBy(
            () -> caso(tickets).ejecutar(organizacion, ticket.id(), EstadoTicket.RESUELTO, Rol.CLIENTE))
        .isInstanceOf(Prohibido.class);

    assertThat(publicador.publicados()).isEmpty();
  }

  @Test
  void transicionIlegalDejaElTicketIgual() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);
    int guardados = tickets.vecesGuardado();

    assertThatThrownBy(
            () ->
                caso(tickets)
                    .ejecutar(organizacion, ticket.id(), EstadoTicket.CERRADO, Rol.AGENTE))
        .isInstanceOf(TransicionNoPermitida.class);

    assertThat(tickets.porId(organizacion, ticket.id()).orElseThrow().estado())
        .isEqualTo(EstadoTicket.ABIERTO);
    assertThat(tickets.vecesGuardado()).isEqualTo(guardados);
  }

  @Test
  void clienteNoPuedeTransicionar() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);

    assertThatThrownBy(
            () ->
                caso(tickets)
                    .ejecutar(organizacion, ticket.id(), EstadoTicket.PENDIENTE, Rol.CLIENTE))
        .isInstanceOf(Prohibido.class);

    assertThat(ticket.estado()).isEqualTo(EstadoTicket.ABIERTO);
  }

  @Test
  void recorreLasAristasPermitidas() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    TransicionarTicket caso = caso(tickets);
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.ALTA, ahora);

    assertThat(caso.ejecutar(organizacion, ticket.id(), EstadoTicket.PENDIENTE, Rol.AGENTE).estado())
        .isEqualTo(EstadoTicket.PENDIENTE);
    assertThat(caso.ejecutar(organizacion, ticket.id(), EstadoTicket.ABIERTO, Rol.ADMIN).estado())
        .isEqualTo(EstadoTicket.ABIERTO);
    assertThat(caso.ejecutar(organizacion, ticket.id(), EstadoTicket.RESUELTO, Rol.AGENTE).estado())
        .isEqualTo(EstadoTicket.RESUELTO);
    assertThat(caso.ejecutar(organizacion, ticket.id(), EstadoTicket.CERRADO, Rol.AGENTE).estado())
        .isEqualTo(EstadoTicket.CERRADO);
    assertThat(tickets.vecesGuardado()).isEqualTo(5);

    Ticket directo = Banco.abrir(tickets, organizacion, autor, Prioridad.BAJA, ahora);
    assertThat(caso.ejecutar(organizacion, directo.id(), EstadoTicket.RESUELTO, Rol.AGENTE).estado())
        .isEqualTo(EstadoTicket.RESUELTO);

    Ticket pendiente = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);
    caso.ejecutar(organizacion, pendiente.id(), EstadoTicket.PENDIENTE, Rol.AGENTE);
    assertThatThrownBy(
            () -> caso.ejecutar(organizacion, pendiente.id(), EstadoTicket.CERRADO, Rol.AGENTE))
        .isInstanceOf(TransicionNoPermitida.class);
    assertThat(caso.ejecutar(organizacion, pendiente.id(), EstadoTicket.RESUELTO, Rol.AGENTE).estado())
        .isEqualTo(EstadoTicket.RESUELTO);

    assertThatThrownBy(
            () -> caso.ejecutar(organizacion, ticket.id(), EstadoTicket.ABIERTO, Rol.AGENTE))
        .isInstanceOf(TransicionNoPermitida.class);
    assertThat(ticket.estado()).isEqualTo(EstadoTicket.CERRADO);
  }
}
