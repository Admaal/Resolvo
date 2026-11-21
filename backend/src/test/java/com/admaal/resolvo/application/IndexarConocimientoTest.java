package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.application.soporte.ArticulosEnMemoria;
import com.admaal.resolvo.application.soporte.Banco;
import com.admaal.resolvo.application.soporte.BaseConocimientoEnMemoria;
import com.admaal.resolvo.application.soporte.EmbebedorFalso;
import com.admaal.resolvo.application.soporte.RepositorioEnMemoria;
import com.admaal.resolvo.domain.Articulo;
import com.admaal.resolvo.domain.ComentarioId;
import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.MotivoModeracion;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IndexarConocimientoTest {

  private static final Instant AHORA = Instant.parse("2026-11-16T10:00:00Z");

  private final OrganizacionId acme = new OrganizacionId(UUID.randomUUID());
  private final OrganizacionId otra = new OrganizacionId(UUID.randomUUID());
  private final UsuarioId autor = new UsuarioId(UUID.randomUUID());
  private final RepositorioEnMemoria tickets = new RepositorioEnMemoria();
  private final ArticulosEnMemoria articulos = new ArticulosEnMemoria();
  private final EmbebedorFalso embebedor = new EmbebedorFalso();
  private final BaseConocimientoEnMemoria base = new BaseConocimientoEnMemoria();
  private final IndexarConocimiento indexar =
      new IndexarConocimiento(embebedor, base, articulos, tickets);

  @Test
  void indexaUnArticuloConSuTituloYOrganizacion() {
    Articulo articulo = new Articulo(UUID.randomUUID(), acme, "Reiniciar router", "Pulsa reset.");

    indexar.articulo(articulo);

    assertThat(base.fragmentosDe(articulo.id()))
        .singleElement()
        .satisfies(
            f -> {
              assertThat(f.titulo()).isEqualTo("Reiniciar router");
              assertThat(f.texto()).isEqualTo("Reiniciar router\nPulsa reset.");
              assertThat(f.organizacionId()).isEqualTo(acme);
            });
  }

  @Test
  void unTextoLargoSeTroceaYCadaFragmentoSeEmbebe() {
    Articulo articulo = new Articulo(UUID.randomUUID(), acme, "Guia", "palabra ".repeat(400));

    indexar.articulo(articulo);

    assertThat(base.fragmentosDe(articulo.id())).hasSizeGreaterThan(1);
    assertThat(embebedor.llamadas()).isEqualTo(base.fragmentosDe(articulo.id()).size());
  }

  @Test
  void reindexarDosVecesNoDuplica() {
    Articulo articulo = new Articulo(UUID.randomUUID(), acme, "Guia", "Contenido");

    indexar.articulo(articulo);
    int fragmentos = base.total();
    int llamadas = embebedor.llamadas();
    indexar.articulo(articulo);

    assertThat(base.total()).isEqualTo(fragmentos);
    assertThat(embebedor.llamadas()).isEqualTo(llamadas);
  }

  @Test
  void cambioDeTextoReemplazaFragmentos() {
    UUID id = UUID.randomUUID();
    indexar.articulo(new Articulo(id, acme, "Guia", "Version vieja"));

    indexar.articulo(new Articulo(id, acme, "Guia", "Version nueva"));

    assertThat(base.fragmentosDe(id)).extracting(Fragmento::texto).containsExactly("Guia\nVersion nueva");
  }

  @Test
  void soloIndexaTicketsResueltosOCerrados() {
    Ticket abierto = Banco.abrir(tickets, acme, autor, Prioridad.ALTA, AHORA);
    Ticket resuelto = Banco.abrir(tickets, acme, autor, Prioridad.ALTA, AHORA);
    resuelto.transicionar(EstadoTicket.RESUELTO, Rol.AGENTE);
    Ticket cerrado = Banco.abrir(tickets, acme, autor, Prioridad.ALTA, AHORA);
    cerrado.transicionar(EstadoTicket.RESUELTO, Rol.AGENTE);
    cerrado.transicionar(EstadoTicket.CERRADO, Rol.AGENTE);

    indexar.ticket(abierto);
    indexar.ticket(resuelto);
    indexar.ticket(cerrado);

    assertThat(base.fragmentosDe(abierto.id().valor())).isEmpty();
    assertThat(base.fragmentosDe(resuelto.id().valor())).hasSize(1);
    assertThat(base.fragmentosDe(cerrado.id().valor())).hasSize(1);
  }

  @Test
  void elTicketIndexadoNoIncluyeComentariosInternos() {
    Ticket ticket = Banco.abrir(tickets, acme, autor, Prioridad.ALTA, AHORA);
    ticket.comentar(ComentarioId.nuevo(), autor, Rol.AGENTE, "Solucion publica", false, AHORA);
    ticket.comentar(ComentarioId.nuevo(), autor, Rol.AGENTE, "Nota interna secreta", true, AHORA);
    ticket.transicionar(EstadoTicket.RESUELTO, Rol.AGENTE);

    indexar.ticket(ticket);

    String texto = base.fragmentosDe(ticket.id().valor()).get(0).texto();
    assertThat(texto).contains(ticket.asunto()).contains(ticket.cuerpo()).contains("Solucion publica");
    assertThat(texto).doesNotContain("Nota interna secreta");
    assertThat(base.fragmentosDe(ticket.id().valor()).get(0).titulo())
        .isEqualTo(ticket.codigo() + " " + ticket.asunto());
  }

  @Test
  void elTicketIndexadoNoIncluyeComentariosOcultos() {
    Ticket ticket = Banco.abrir(tickets, acme, autor, Prioridad.ALTA, AHORA);
    ticket.comentar(ComentarioId.nuevo(), autor, Rol.AGENTE, "Solucion publica", false, AHORA);
    ComentarioId spam = ComentarioId.nuevo();
    ticket.comentar(spam, autor, Rol.CLIENTE, "Compra pastillas ya", false, AHORA);
    ticket.ocultarComentario(spam, autor, Rol.AGENTE, MotivoModeracion.SPAM, AHORA);
    ticket.transicionar(EstadoTicket.RESUELTO, Rol.AGENTE);

    indexar.ticket(ticket);

    String texto = base.fragmentosDe(ticket.id().valor()).get(0).texto();
    assertThat(texto).contains("Solucion publica");
    assertThat(texto).doesNotContain("Compra pastillas ya").doesNotContain("null");
  }

  @Test
  void indexarLaOrganizacionRecorreArticulosYTicketsResueltosSoloDeEsaOrganizacion() {
    Articulo propio = new Articulo(UUID.randomUUID(), acme, "Propio", "texto");
    Articulo ajeno = new Articulo(UUID.randomUUID(), otra, "Ajeno", "texto");
    articulos.poner(propio);
    articulos.poner(ajeno);
    Ticket resuelto = Banco.abrir(tickets, acme, autor, Prioridad.ALTA, AHORA);
    resuelto.transicionar(EstadoTicket.RESUELTO, Rol.AGENTE);
    Ticket abierto = Banco.abrir(tickets, acme, autor, Prioridad.ALTA, AHORA);

    indexar.organizacion(acme);

    assertThat(base.fragmentosDe(propio.id())).hasSize(1);
    assertThat(base.fragmentosDe(ajeno.id())).isEmpty();
    assertThat(base.fragmentosDe(resuelto.id().valor())).hasSize(1);
    assertThat(base.fragmentosDe(abierto.id().valor())).isEmpty();
  }
}
