package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.Banco;
import com.admaal.resolvo.application.soporte.RepositorioEnMemoria;
import com.admaal.resolvo.application.soporte.UsuariosEnMemoria;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.Usuario;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AsignarTicketTest {

  private final Instant ahora = Instant.parse("2026-11-04T10:00:00Z");
  private final OrganizacionId organizacion = new OrganizacionId(UUID.randomUUID());
  private final UsuarioId autor = new UsuarioId(UUID.randomUUID());

  @Test
  void clienteNoPuedeAsignar() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);
    int guardados = tickets.vecesGuardado();

    assertThatThrownBy(
            () ->
                new AsignarTicket(tickets, new UsuariosEnMemoria())
                    .ejecutar(organizacion, ticket.id(), new UsuarioId(UUID.randomUUID()), autor, Rol.CLIENTE))
        .isInstanceOf(Prohibido.class);

    assertThat(ticket.asignadoId()).isNull();
    assertThat(tickets.vecesGuardado()).isEqualTo(guardados);
  }

  @Test
  void asignaAUnAgenteDeLaMismaOrganizacion() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    UsuariosEnMemoria usuarios = new UsuariosEnMemoria();
    Usuario agente = agente(organizacion);
    usuarios.guardar(agente);
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.ALTA, ahora);

    Ticket asignado =
        new AsignarTicket(tickets, usuarios)
            .ejecutar(organizacion, ticket.id(), agente.id(), autor, Rol.AGENTE);

    assertThat(asignado.asignadoId()).isEqualTo(agente.id());
    assertThat(tickets.vecesGuardado()).isEqualTo(2);
  }

  @Test
  void noAsignaAUnCliente() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    UsuariosEnMemoria usuarios = new UsuariosEnMemoria();
    Usuario cliente =
        Usuario.nuevo(
            new UsuarioId(UUID.randomUUID()), organizacion, "otro@acme.test", Rol.CLIENTE, "hash");
    usuarios.guardar(cliente);
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.ALTA, ahora);

    assertThatThrownBy(
            () ->
                new AsignarTicket(tickets, usuarios)
                    .ejecutar(organizacion, ticket.id(), cliente.id(), autor, Rol.AGENTE))
        .isInstanceOf(NoEncontrado.class);
  }

  @Test
  void agenteDeOtraOrganizacionNoExiste() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    UsuariosEnMemoria usuarios = new UsuariosEnMemoria();
    usuarios.guardar(agente(new OrganizacionId(UUID.randomUUID())));
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.ALTA, ahora);
    UsuarioId ajeno = usuarios.porEmail("agente@acme.test").orElseThrow().id();

    assertThatThrownBy(
            () ->
                new AsignarTicket(tickets, usuarios)
                    .ejecutar(organizacion, ticket.id(), ajeno, autor, Rol.ADMIN))
        .isInstanceOf(NoEncontrado.class);
  }

  private static Usuario agente(OrganizacionId organizacion) {
    return Usuario.nuevo(
        new UsuarioId(UUID.randomUUID()),
        organizacion,
        "agente@acme.test",
        Rol.AGENTE,
        "hash");
  }

  @Test
  void unClienteAjenoRecibeNoEncontradoYElAutorProhibido() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);
    UsuarioId otro = new UsuarioId(UUID.randomUUID());
    UsuarioId destino = new UsuarioId(UUID.randomUUID());
    AsignarTicket caso = new AsignarTicket(tickets, new UsuariosEnMemoria());

    assertThatThrownBy(() -> caso.ejecutar(organizacion, ticket.id(), destino, otro, Rol.CLIENTE))
        .isInstanceOf(NoEncontrado.class);
    assertThatThrownBy(() -> caso.ejecutar(organizacion, ticket.id(), destino, autor, Rol.CLIENTE))
        .isInstanceOf(Prohibido.class);
    assertThat(ticket.asignadoId()).isNull();
  }
}
