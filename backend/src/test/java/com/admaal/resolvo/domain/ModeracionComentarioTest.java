package com.admaal.resolvo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModeracionComentarioTest {

  private final Instant ahora = Instant.parse("2026-11-21T10:00:00Z");
  private final UsuarioId cliente = new UsuarioId(UUID.randomUUID());
  private final UsuarioId agente = new UsuarioId(UUID.randomUUID());
  private final UsuarioId admin = new UsuarioId(UUID.randomUUID());
  private final Ticket ticket =
      Ticket.abrir(
          TicketId.nuevo(),
          new OrganizacionId(UUID.randomUUID()),
          cliente,
          new CategoriaId(UUID.randomUUID()),
          Prioridad.MEDIA,
          "RS-1",
          "Asunto",
          "Cuerpo",
          ahora,
          ahora.plusSeconds(3600));

  private Comentario comentar(UsuarioId autor, Rol rol, String texto, boolean interno) {
    return ticket.comentar(ComentarioId.nuevo(), autor, rol, texto, interno, ahora);
  }

  @Test
  void unAgenteOcultaUnComentarioConMotivo() {
    Comentario spam = comentar(cliente, Rol.CLIENTE, "Compra ya", false);
    Instant cuando = ahora.plusSeconds(60);

    Comentario oculto =
        ticket.ocultarComentario(spam.id(), agente, Rol.AGENTE, MotivoModeracion.SPAM, cuando);

    assertThat(oculto.retirado()).isTrue();
    assertThat(oculto.moderacion().motivo()).isEqualTo(MotivoModeracion.SPAM);
    assertThat(oculto.moderacion().por()).isEqualTo(agente);
    assertThat(oculto.moderacion().en()).isEqualTo(cuando);
    assertThat(oculto.cuerpo()).isEqualTo("Compra ya");
    assertThat(ticket.visiblesPara(Rol.AGENTE)).containsExactly(oculto);
  }

  @Test
  void unClienteNoPuedeOcultar() {
    Comentario comentario = comentar(cliente, Rol.CLIENTE, "Hola", false);

    assertThatThrownBy(
            () ->
                ticket.ocultarComentario(
                    comentario.id(), cliente, Rol.CLIENTE, MotivoModeracion.OTRO, ahora))
        .isInstanceOf(Prohibido.class);
    assertThat(ticket.visiblesPara(Rol.AGENTE).get(0).retirado()).isFalse();
  }

  @Test
  void ocultarSinMotivoEsInvalidoYNoOculta() {
    Comentario comentario = comentar(cliente, Rol.CLIENTE, "Hola", false);

    assertThatThrownBy(
            () -> ticket.ocultarComentario(comentario.id(), agente, Rol.AGENTE, null, ahora))
        .isInstanceOf(TextoInvalido.class);
    assertThat(ticket.visiblesPara(Rol.AGENTE).get(0).retirado()).isFalse();
  }

  @Test
  void ocultarUnComentarioInexistenteEsNoEncontrado() {
    assertThatThrownBy(
            () ->
                ticket.ocultarComentario(
                    ComentarioId.nuevo(), agente, Rol.AGENTE, MotivoModeracion.SPAM, ahora))
        .isInstanceOf(NoEncontrado.class);
  }

  @Test
  void ocultarDosVecesConservaLaPrimeraModeracion() {
    Comentario comentario = comentar(cliente, Rol.CLIENTE, "Feo", false);
    Instant primera = ahora.plusSeconds(60);
    ticket.ocultarComentario(
        comentario.id(), agente, Rol.AGENTE, MotivoModeracion.OFENSIVO, primera);

    Comentario segunda =
        ticket.ocultarComentario(
            comentario.id(), admin, Rol.ADMIN, MotivoModeracion.SPAM, primera.plusSeconds(60));

    assertThat(segunda.moderacion().motivo()).isEqualTo(MotivoModeracion.OFENSIVO);
    assertThat(segunda.moderacion().por()).isEqualTo(agente);
    assertThat(segunda.moderacion().en()).isEqualTo(primera);
  }

  @Test
  void soloAdminRestaura() {
    Comentario comentario = comentar(cliente, Rol.CLIENTE, "Feo", false);
    ticket.ocultarComentario(
        comentario.id(), agente, Rol.AGENTE, MotivoModeracion.OFENSIVO, ahora);

    assertThatThrownBy(() -> ticket.restaurarComentario(comentario.id(), Rol.AGENTE))
        .isInstanceOf(Prohibido.class);
    assertThatThrownBy(() -> ticket.restaurarComentario(comentario.id(), Rol.CLIENTE))
        .isInstanceOf(Prohibido.class);
    assertThat(ticket.visiblesPara(Rol.AGENTE).get(0).retirado()).isTrue();

    Comentario restaurado = ticket.restaurarComentario(comentario.id(), Rol.ADMIN);

    assertThat(restaurado.retirado()).isFalse();
    assertThat(restaurado.moderacion()).isNull();
    assertThat(restaurado.cuerpo()).isEqualTo("Feo");
  }

  @Test
  void restaurarUnComentarioInexistenteEsNoEncontrado() {
    assertThatThrownBy(() -> ticket.restaurarComentario(ComentarioId.nuevo(), Rol.ADMIN))
        .isInstanceOf(NoEncontrado.class);
  }

  @Test
  void elClienteVeElComentarioOcultoSinCuerpo() {
    Comentario visible = comentar(agente, Rol.AGENTE, "Probad otra vez", false);
    Comentario spam = comentar(cliente, Rol.CLIENTE, "Compra ya http://x.test", false);
    ticket.ocultarComentario(spam.id(), agente, Rol.AGENTE, MotivoModeracion.SPAM, ahora);

    var paraElCliente = ticket.visiblesPara(Rol.CLIENTE);

    assertThat(paraElCliente).hasSize(2);
    assertThat(paraElCliente.get(0)).isEqualTo(visible);
    assertThat(paraElCliente.get(1).id()).isEqualTo(spam.id());
    assertThat(paraElCliente.get(1).retirado()).isTrue();
    assertThat(paraElCliente.get(1).cuerpo()).isNull();
  }

  @Test
  void elClienteNoSabeQuienNiPorQueSeOculto() {
    Comentario spam = comentar(cliente, Rol.CLIENTE, "Compra ya", false);
    ticket.ocultarComentario(spam.id(), agente, Rol.AGENTE, MotivoModeracion.OFENSIVO, ahora);

    Moderacion visible = ticket.visiblesPara(Rol.CLIENTE).get(0).moderacion();

    assertThat(visible).isNotNull();
    assertThat(visible.motivo()).isNull();
    assertThat(visible.por()).isNull();
    assertThat(visible.en()).isNull();
    assertThat(ticket.visiblesPara(Rol.AGENTE).get(0).moderacion().por()).isEqualTo(agente);
  }

  @Test
  void unaNotaInternaOcultaSigueSinVerseParaElCliente() {
    Comentario nota = comentar(agente, Rol.AGENTE, "Nota", true);
    ticket.ocultarComentario(nota.id(), agente, Rol.AGENTE, MotivoModeracion.OTRO, ahora);

    assertThat(ticket.visiblesPara(Rol.CLIENTE)).isEmpty();
  }

  @Test
  void elAgenteVeElTextoDelComentarioOculto() {
    Comentario spam = comentar(cliente, Rol.CLIENTE, "Compra ya", false);
    ticket.ocultarComentario(spam.id(), agente, Rol.AGENTE, MotivoModeracion.SPAM, ahora);

    assertThat(ticket.visiblesPara(Rol.AGENTE).get(0).cuerpo()).isEqualTo("Compra ya");
    assertThat(ticket.visiblesPara(Rol.ADMIN).get(0).cuerpo()).isEqualTo("Compra ya");
  }

  @Test
  void todosIncluyeLosOcultosYLasNotasInternas() {
    Comentario nota = comentar(agente, Rol.AGENTE, "Nota", true);
    Comentario spam = comentar(cliente, Rol.CLIENTE, "Compra ya", false);
    ticket.ocultarComentario(spam.id(), agente, Rol.AGENTE, MotivoModeracion.SPAM, ahora);

    assertThat(ticket.todos()).extracting(Comentario::id).containsExactly(nota.id(), spam.id());
    assertThat(ticket.todos().get(1).cuerpo()).isEqualTo("Compra ya");
  }

  @Test
  void unClienteConMasDeTresEnlacesEsRechazado() {
    String cuatro = "http://a.test https://b.test www.c.test HTTP://D.TEST";

    assertThatThrownBy(() -> comentar(cliente, Rol.CLIENTE, cuatro, false))
        .isInstanceOf(DemasiadosEnlaces.class)
        .hasMessage("demasiados-enlaces");
    assertThat(ticket.todos()).isEmpty();
  }

  @Test
  void unClienteConTresEnlacesPuedeComentar() {
    String tres = "mira http://a.test y https://b.test y www.c.test";

    assertThat(comentar(cliente, Rol.CLIENTE, tres, false).cuerpo()).isEqualTo(tres);
  }

  @Test
  void unEnlaceConProtocoloYWwwCuentaUnaSolaVez() {
    String tres = "http://www.a.test https://www.b.test http://www.c.test";

    assertThat(comentar(cliente, Rol.CLIENTE, tres, false).cuerpo()).isEqualTo(tres);
  }

  @Test
  void unEnlacePegadoAUnCaracterDePalabraCuentaIgual() {
    String cuatro = "1https://a.test _https://b.test xhttp://c.test yhttp://d.test";

    assertThatThrownBy(() -> comentar(cliente, Rol.CLIENTE, cuatro, false))
        .isInstanceOf(DemasiadosEnlaces.class);
  }

  @Test
  void dosEnlacesSinEspacioEntreEllosCuentanPorSeparado() {
    String cuatro = "http://a.testhttp://b.test http://c.test http://d.test";

    assertThatThrownBy(() -> comentar(cliente, Rol.CLIENTE, cuatro, false))
        .isInstanceOf(DemasiadosEnlaces.class);
  }

  @Test
  void unAgentePuedePegarEnlaces() {
    String muchos = "http://a.test http://b.test http://c.test http://d.test http://e.test";

    assertThat(comentar(agente, Rol.AGENTE, muchos, false).cuerpo()).isEqualTo(muchos);
  }
}
