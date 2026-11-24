package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.Banco;
import com.admaal.resolvo.application.soporte.RelojManual;
import com.admaal.resolvo.application.soporte.RepositorioEnMemoria;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.TextoInvalido;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ComentarTicketTest {

  private final Instant ahora = Instant.parse("2026-11-04T10:00:00Z");
  private final OrganizacionId organizacion = new OrganizacionId(UUID.randomUUID());
  private final UsuarioId autor = new UsuarioId(UUID.randomUUID());
  private final UsuarioId agente = new UsuarioId(UUID.randomUUID());

  @Test
  void clienteNoVeLaNotaInterna() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);
    ComentarTicket caso = new ComentarTicket(tickets, new RelojManual(ahora));
    ConsultarTicket consulta = new ConsultarTicket(tickets);

    caso.ejecutar(organizacion, ticket.id(), agente, Rol.AGENTE, "Nota para el equipo", true);
    caso.ejecutar(organizacion, ticket.id(), autor, Rol.CLIENTE, "Sigue sin imprimir", false);

    assertThat(consulta.comentarios(organizacion, ticket.id(), autor, Rol.CLIENTE))
        .extracting(comentario -> comentario.cuerpo())
        .containsExactly("Sigue sin imprimir");
    assertThat(consulta.comentarios(organizacion, ticket.id(), agente, Rol.AGENTE)).hasSize(2);
    assertThat(tickets.vecesGuardado()).isEqualTo(3);
  }

  @Test
  void conservaElMarcadoComoTexto() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.BAJA, ahora);
    String marcado = "<script>alert(1)</script>";

    var comentario =
        new ComentarTicket(tickets, new RelojManual(ahora))
            .ejecutar(organizacion, ticket.id(), autor, Rol.CLIENTE, marcado, false);

    assertThat(comentario.cuerpo()).isEqualTo(marcado);
  }

  @Test
  void rechazaElCuerpoVacio() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.BAJA, ahora);
    ComentarTicket caso = new ComentarTicket(tickets, new RelojManual(ahora));

    assertThatThrownBy(
            () -> caso.ejecutar(organizacion, ticket.id(), autor, Rol.CLIENTE, "   ", false))
        .isInstanceOf(TextoInvalido.class);
    assertThatThrownBy(
            () -> caso.ejecutar(organizacion, ticket.id(), autor, Rol.CLIENTE, null, false))
        .isInstanceOf(TextoInvalido.class);
    assertThatThrownBy(
            () ->
                caso.ejecutar(
                    organizacion, ticket.id(), agente, Rol.AGENTE, "a".repeat(8001), false))
        .isInstanceOf(TextoInvalido.class);
    assertThat(
            caso.ejecutar(organizacion, ticket.id(), agente, Rol.AGENTE, "a".repeat(8000), false)
                .cuerpo())
        .hasSize(8000);
  }

  @Test
  void clienteNoEscribeNotaInterna() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);

    assertThatThrownBy(
            () ->
                new ComentarTicket(tickets, new RelojManual(ahora))
                    .ejecutar(organizacion, ticket.id(), autor, Rol.CLIENTE, "Nota", true))
        .isInstanceOf(Prohibido.class);
  }

  @Test
  void unClienteNoComentaEnElTicketDeOtroNiLoSabeElTicket() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);
    UsuarioId otro = new UsuarioId(UUID.randomUUID());
    int guardados = tickets.vecesGuardado();

    assertThatThrownBy(
            () ->
                new ComentarTicket(tickets, new RelojManual(ahora))
                    .ejecutar(organizacion, ticket.id(), otro, Rol.CLIENTE, "Intruso", false))
        .isInstanceOf(NoEncontrado.class);

    assertThat(ticket.todos()).isEmpty();
    assertThat(tickets.vecesGuardado()).isEqualTo(guardados);
  }

  @Test
  void unClienteAjenoRecibeNoEncontradoAunqueLaNotaSeaInterna() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Ticket ticket = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);

    assertThatThrownBy(
            () ->
                new ComentarTicket(tickets, new RelojManual(ahora))
                    .ejecutar(
                        organizacion, ticket.id(), new UsuarioId(UUID.randomUUID()), Rol.CLIENTE, "x", true))
        .isInstanceOf(NoEncontrado.class);
  }
}
