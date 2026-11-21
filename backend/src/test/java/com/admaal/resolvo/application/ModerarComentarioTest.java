package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.Banco;
import com.admaal.resolvo.application.soporte.RelojManual;
import com.admaal.resolvo.application.soporte.RepositorioEnMemoria;
import com.admaal.resolvo.domain.Comentario;
import com.admaal.resolvo.domain.ComentarioId;
import com.admaal.resolvo.domain.MotivoModeracion;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModerarComentarioTest {

  private final Instant ahora = Instant.parse("2026-11-21T10:00:00Z");
  private final OrganizacionId organizacion = new OrganizacionId(UUID.randomUUID());
  private final UsuarioId cliente = new UsuarioId(UUID.randomUUID());
  private final UsuarioId agente = new UsuarioId(UUID.randomUUID());
  private final RepositorioEnMemoria tickets = new RepositorioEnMemoria();
  private final Ticket ticket = Banco.abrir(tickets, organizacion, cliente, Prioridad.MEDIA, ahora);
  private final ModerarComentario caso = new ModerarComentario(tickets, new RelojManual(ahora));

  private Comentario spam() {
    return new ComentarTicket(tickets, new RelojManual(ahora))
        .ejecutar(organizacion, ticket.id(), cliente, Rol.CLIENTE, "Compra ya", false);
  }

  @Test
  void ocultaYGuardaElComentario() {
    Comentario comentario = spam();
    int guardadosAntes = tickets.vecesGuardado();

    Comentario oculto =
        caso.ocultar(
            organizacion, ticket.id(), comentario.id(), agente, Rol.AGENTE, MotivoModeracion.SPAM);

    assertThat(oculto.retirado()).isTrue();
    assertThat(oculto.moderacion().en()).isEqualTo(ahora);
    assertThat(tickets.vecesGuardado()).isEqualTo(guardadosAntes + 1);
    ConsultarTicket consulta = new ConsultarTicket(tickets);
    assertThat(consulta.comentarios(organizacion, ticket.id(), Rol.AGENTE).get(0).retirado())
        .isTrue();
    assertThat(consulta.comentarios(organizacion, ticket.id(), Rol.CLIENTE).get(0).cuerpo())
        .isNull();
  }

  @Test
  void unClienteNoOculta() {
    Comentario comentario = spam();

    assertThatThrownBy(
            () ->
                caso.ocultar(
                    organizacion,
                    ticket.id(),
                    comentario.id(),
                    cliente,
                    Rol.CLIENTE,
                    MotivoModeracion.OTRO))
        .isInstanceOf(Prohibido.class);
  }

  @Test
  void soloAdminRestauraYSeGuarda() {
    Comentario comentario = spam();
    caso.ocultar(
        organizacion, ticket.id(), comentario.id(), agente, Rol.AGENTE, MotivoModeracion.SPAM);
    int guardadosAntes = tickets.vecesGuardado();

    assertThatThrownBy(
            () -> caso.restaurar(organizacion, ticket.id(), comentario.id(), Rol.AGENTE))
        .isInstanceOf(Prohibido.class);
    assertThat(tickets.vecesGuardado()).isEqualTo(guardadosAntes);

    Comentario restaurado = caso.restaurar(organizacion, ticket.id(), comentario.id(), Rol.ADMIN);

    assertThat(restaurado.retirado()).isFalse();
    assertThat(tickets.vecesGuardado()).isEqualTo(guardadosAntes + 1);
  }

  @Test
  void unTicketDeOtraOrganizacionNoSeEncuentra() {
    Comentario comentario = spam();
    OrganizacionId otra = new OrganizacionId(UUID.randomUUID());

    assertThatThrownBy(
            () ->
                caso.ocultar(
                    otra, ticket.id(), comentario.id(), agente, Rol.AGENTE, MotivoModeracion.SPAM))
        .isInstanceOf(NoEncontrado.class);
    assertThatThrownBy(() -> caso.restaurar(otra, ticket.id(), comentario.id(), Rol.ADMIN))
        .isInstanceOf(NoEncontrado.class);
  }

  @Test
  void unComentarioInexistenteNoSeEncuentra() {
    assertThatThrownBy(
            () ->
                caso.ocultar(
                    organizacion,
                    ticket.id(),
                    ComentarioId.nuevo(),
                    agente,
                    Rol.AGENTE,
                    MotivoModeracion.SPAM))
        .isInstanceOf(NoEncontrado.class);
  }
}
