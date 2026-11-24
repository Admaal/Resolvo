package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.Banco;
import com.admaal.resolvo.application.soporte.RepositorioEnMemoria;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarTicketTest {

  @Test
  void otraOrganizacionEsIndistinguibleDeNoExistir() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    OrganizacionId organizacion = new OrganizacionId(UUID.randomUUID());
    UsuarioId autor = new UsuarioId(UUID.randomUUID());
    Ticket ticket =
        Banco.abrir(
            tickets, organizacion, autor, Prioridad.MEDIA, Instant.parse("2026-11-04T10:00:00Z"));
    ConsultarTicket caso = new ConsultarTicket(tickets);

    assertThatThrownBy(
            () ->
                caso.ejecutar(
                    new OrganizacionId(UUID.randomUUID()), ticket.id(), autor, Rol.AGENTE))
        .isInstanceOf(NoEncontrado.class);
    assertThatThrownBy(() -> caso.ejecutar(organizacion, TicketId.nuevo(), autor, Rol.AGENTE))
        .isInstanceOf(NoEncontrado.class);
  }

  private final RepositorioEnMemoria tickets = new RepositorioEnMemoria();
  private final OrganizacionId organizacion = new OrganizacionId(UUID.randomUUID());

  private final UsuarioId autor = new UsuarioId(UUID.randomUUID());
  private final UsuarioId otroCliente = new UsuarioId(UUID.randomUUID());

  private Ticket abrirRs1() {
    return Banco.abrir(
        tickets,
        organizacion,
        autor,
        Prioridad.MEDIA,
        Instant.parse("2026-11-04T10:00:00Z"));
  }

  @Test
  void porCodigoEncuentraElTicketDeLaOrganizacion() {
    Ticket ticket = abrirRs1();

    assertThat(new ConsultarTicket(tickets).porCodigo(organizacion, "RS-1", autor, Rol.CLIENTE).id())
        .isEqualTo(ticket.id());
  }

  @Test
  void elCodigoNoDistingueMayusculas() {
    Ticket ticket = abrirRs1();

    assertThat(new ConsultarTicket(tickets).porCodigo(organizacion, "rs-1", autor, Rol.CLIENTE).id())
        .isEqualTo(ticket.id());
  }

  @Test
  void elCodigoDeOtraOrganizacionEsIndistinguibleDeNoExistir() {
    abrirRs1();
    ConsultarTicket caso = new ConsultarTicket(tickets);

    assertThatThrownBy(() -> caso.porCodigo(new OrganizacionId(UUID.randomUUID()), "RS-1", autor, Rol.AGENTE))
        .isInstanceOf(NoEncontrado.class);
  }

  @Test
  void unCodigoQueNoExisteNoSeEncuentra() {
    abrirRs1();
    ConsultarTicket caso = new ConsultarTicket(tickets);

    assertThatThrownBy(() -> caso.porCodigo(organizacion, "RS-99", autor, Rol.AGENTE)).isInstanceOf(NoEncontrado.class);
    assertThatThrownBy(() -> caso.porCodigo(organizacion, "abc", autor, Rol.AGENTE)).isInstanceOf(NoEncontrado.class);
    assertThatThrownBy(() -> caso.porCodigo(organizacion, "", autor, Rol.AGENTE)).isInstanceOf(NoEncontrado.class);
  }

  @Test
  void unClienteNoAbreElTicketDeOtroClienteSeaPorIdOPorCodigo() {
    Ticket ticket = abrirRs1();
    ConsultarTicket caso = new ConsultarTicket(tickets);

    assertThatThrownBy(() -> caso.ejecutar(organizacion, ticket.id(), otroCliente, Rol.CLIENTE))
        .isInstanceOf(NoEncontrado.class);
    assertThatThrownBy(() -> caso.porCodigo(organizacion, "RS-1", otroCliente, Rol.CLIENTE))
        .isInstanceOf(NoEncontrado.class);
  }

  @Test
  void unClienteNoLeeLosComentariosDelTicketDeOtro() {
    Ticket ticket = abrirRs1();

    assertThatThrownBy(
            () ->
                new ConsultarTicket(tickets)
                    .comentarios(organizacion, ticket.id(), otroCliente, Rol.CLIENTE))
        .isInstanceOf(NoEncontrado.class);
  }

  @Test
  void elAutorAbreSuTicketYUnAgenteOUnAdminAbrenCualquiera() {
    Ticket ticket = abrirRs1();
    ConsultarTicket caso = new ConsultarTicket(tickets);

    assertThat(caso.ejecutar(organizacion, ticket.id(), autor, Rol.CLIENTE).id())
        .isEqualTo(ticket.id());
    assertThat(caso.ejecutar(organizacion, ticket.id(), otroCliente, Rol.AGENTE).id())
        .isEqualTo(ticket.id());
    assertThat(caso.porCodigo(organizacion, "RS-1", otroCliente, Rol.ADMIN).id())
        .isEqualTo(ticket.id());
    assertThat(caso.comentarios(organizacion, ticket.id(), otroCliente, Rol.AGENTE)).isEmpty();
  }
}
