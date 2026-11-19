package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.application.soporte.BandejaEnMemoria;
import com.admaal.resolvo.application.soporte.BandejaEnMemoria.Estado;
import com.admaal.resolvo.application.soporte.Banco;
import com.admaal.resolvo.application.soporte.BaseConocimientoEnMemoria;
import com.admaal.resolvo.application.soporte.EmbebedorFalso;
import com.admaal.resolvo.application.soporte.ProcesadosEnMemoria;
import com.admaal.resolvo.application.soporte.RepositorioEnMemoria;
import com.admaal.resolvo.application.soporte.ArticulosEnMemoria;
import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.EventoTicket;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TipoEvento;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class ProcesarEventosTest {

  private static final Instant AHORA = Instant.parse("2026-11-19T10:00:00Z");
  private static final int MAX_INTENTOS = 5;

  private final OrganizacionId acme = new OrganizacionId(UUID.randomUUID());
  private final UsuarioId autor = new UsuarioId(UUID.randomUUID());
  private final BandejaEnMemoria bandeja = new BandejaEnMemoria();
  private final ProcesadosEnMemoria procesados = new ProcesadosEnMemoria();

  /** Un consumidor que cuenta cuantas veces le llegan eventos y puede fallar a voluntad. */
  private static final class Contador implements ConsumidorEventos {
    final String nombre;
    final TipoEvento tipo;
    final List<UUID> recibidos = new ArrayList<>();
    Consumer<EventoTicket> accion = evento -> {};

    Contador(String nombre, TipoEvento tipo) {
      this.nombre = nombre;
      this.tipo = tipo;
    }

    @Override
    public String nombre() {
      return nombre;
    }

    @Override
    public boolean admite(TipoEvento tipo) {
      return this.tipo == tipo;
    }

    @Override
    public void consumir(EventoTicket evento) {
      recibidos.add(evento.id());
      accion.accept(evento);
    }
  }

  private ProcesarEventos procesador(ConsumidorEventos... consumidores) {
    return new ProcesarEventos(bandeja, procesados, List.of(consumidores), MAX_INTENTOS, 10);
  }

  private EventoTicket evento(TipoEvento tipo) {
    EventoTicket evento =
        EventoTicket.nuevo(
            tipo, acme, new com.admaal.resolvo.domain.TicketId(UUID.randomUUID()), AHORA);
    bandeja.agregar(evento);
    return evento;
  }

  @Test
  void unEventoProcesadoSeMarcaHechoYSuConsumidorLoRecibeUnaVez() {
    Contador indexar = new Contador("indexar", TipoEvento.TICKET_RESUELTO);
    EventoTicket evento = evento(TipoEvento.TICKET_RESUELTO);

    ProcesarEventos.Resultado resultado = procesador(indexar).ejecutar();

    assertThat(indexar.recibidos).containsExactly(evento.id());
    assertThat(bandeja.estadoDe(evento.id())).isEqualTo(Estado.HECHO);
    assertThat(resultado.hechos()).isEqualTo(1);
    assertThat(resultado.reintentables()).isZero();
  }

  @Test
  void elMismoEventoEntregadoDosVecesLlegaUnaSolaVezAlConsumidor() {
    Contador indexar = new Contador("indexar", TipoEvento.TICKET_RESUELTO);
    EventoTicket evento = evento(TipoEvento.TICKET_RESUELTO);
    procesador(indexar).ejecutar();

    bandeja.reentregar(evento.id());
    ProcesarEventos.Resultado segunda = procesador(indexar).ejecutar();

    assertThat(indexar.recibidos).hasSize(1);
    assertThat(bandeja.estadoDe(evento.id())).isEqualTo(Estado.HECHO);
    assertThat(segunda.hechos()).isEqualTo(1);
  }

  @Test
  void elMismoEventoDosVecesNoReindexaElTicket() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    EmbebedorFalso embebedor = new EmbebedorFalso();
    BaseConocimientoEnMemoria base = new BaseConocimientoEnMemoria();
    IndexarConocimiento indexar =
        new IndexarConocimiento(embebedor, base, new ArticulosEnMemoria(), tickets);
    Ticket ticket = Banco.abrir(tickets, acme, autor, Prioridad.ALTA, AHORA);
    ticket.transicionar(EstadoTicket.RESUELTO, Rol.AGENTE);
    EventoTicket evento =
        EventoTicket.nuevo(TipoEvento.TICKET_RESUELTO, acme, ticket.id(), AHORA);
    bandeja.agregar(evento);
    ProcesarEventos proceso =
        procesador(new IndexarTicketResuelto(indexar, tickets));

    proceso.ejecutar();
    int llamadas = embebedor.llamadas();
    bandeja.reentregar(evento.id());
    proceso.ejecutar();

    assertThat(base.fragmentosDe(ticket.id().valor())).hasSize(1);
    assertThat(embebedor.llamadas()).isEqualTo(llamadas);
  }

  @Test
  void unConsumidorQueFallaDejaElEventoParaReintentarYAlQuintoIntentoFalla() {
    Contador roto = new Contador("indexar", TipoEvento.TICKET_RESUELTO);
    roto.accion =
        evento -> {
          throw new IllegalStateException("boom");
        };
    EventoTicket evento = evento(TipoEvento.TICKET_RESUELTO);
    ProcesarEventos proceso = procesador(roto);

    for (int intento = 1; intento < MAX_INTENTOS; intento++) {
      ProcesarEventos.Resultado resultado = proceso.ejecutar();
      assertThat(resultado.reintentables()).isEqualTo(1);
      assertThat(bandeja.estadoDe(evento.id())).isEqualTo(Estado.PENDIENTE);
      assertThat(bandeja.intentosDe(evento.id())).isEqualTo(intento);
      assertThat(bandeja.errorDe(evento.id())).contains("boom");
      bandeja.vencerReclamos();
    }
    ProcesarEventos.Resultado ultimo = proceso.ejecutar();

    assertThat(bandeja.estadoDe(evento.id())).isEqualTo(Estado.FALLIDO);
    assertThat(bandeja.intentosDe(evento.id())).isEqualTo(MAX_INTENTOS);
    assertThat(bandeja.errorDe(evento.id())).contains("boom");
    assertThat(ultimo.fallidos()).isEqualTo(1);
    assertThat(ultimo.reintentables()).isZero();
    assertThat(roto.recibidos).hasSize(MAX_INTENTOS);
  }

  @Test
  void unEventoQueFallaNoImpideProcesarLosSiguientes() {
    Contador consumidor = new Contador("indexar", TipoEvento.TICKET_RESUELTO);
    EventoTicket malo = evento(TipoEvento.TICKET_RESUELTO);
    EventoTicket bueno = evento(TipoEvento.TICKET_RESUELTO);
    consumidor.accion =
        e -> {
          if (e.id().equals(malo.id())) {
            throw new IllegalStateException("falla el primero");
          }
        };

    ProcesarEventos.Resultado resultado = procesador(consumidor).ejecutar();

    assertThat(bandeja.estadoDe(malo.id())).isEqualTo(Estado.PENDIENTE);
    assertThat(bandeja.estadoDe(bueno.id())).isEqualTo(Estado.HECHO);
    assertThat(resultado.hechos()).isEqualTo(1);
    assertThat(resultado.reintentables()).isEqualTo(1);
  }

  @Test
  void alReintentarNoSeRepiteUnConsumidorQueYaTuvoExito() {
    Contador primero = new Contador("a", TipoEvento.TICKET_RESUELTO);
    Contador segundo = new Contador("b", TipoEvento.TICKET_RESUELTO);
    boolean[] falla = {true};
    segundo.accion =
        e -> {
          if (falla[0]) {
            throw new IllegalStateException("b cae una vez");
          }
        };
    EventoTicket evento = evento(TipoEvento.TICKET_RESUELTO);
    ProcesarEventos proceso = procesador(primero, segundo);

    proceso.ejecutar();
    falla[0] = false;
    bandeja.vencerReclamos();
    proceso.ejecutar();

    assertThat(primero.recibidos).hasSize(1);
    assertThat(segundo.recibidos).hasSize(2);
    assertThat(bandeja.estadoDe(evento.id())).isEqualTo(Estado.HECHO);
  }

  @Test
  void sinConsumidorQueAdmitaElTipoElEventoSeMarcaHecho() {
    Contador soloReabiertos = new Contador("desindexar", TipoEvento.TICKET_REABIERTO);
    EventoTicket evento = evento(TipoEvento.TICKET_RESUELTO);

    ProcesarEventos.Resultado resultado = procesador(soloReabiertos).ejecutar();

    assertThat(soloReabiertos.recibidos).isEmpty();
    assertThat(bandeja.estadoDe(evento.id())).isEqualTo(Estado.HECHO);
    assertThat(resultado.hechos()).isEqualTo(1);
  }

  @Test
  void sinConsumidoresLaBandejaSeVacia() {
    EventoTicket evento = evento(TipoEvento.TICKET_REABIERTO);

    procesador().ejecutar();

    assertThat(bandeja.estadoDe(evento.id())).isEqualTo(Estado.HECHO);
  }

  @Test
  void elLoteSeAgotaAunqueHayaMasEventosQueTamanoDeLote() {
    Contador consumidor = new Contador("indexar", TipoEvento.TICKET_RESUELTO);
    for (int i = 0; i < 25; i++) {
      evento(TipoEvento.TICKET_RESUELTO);
    }

    ProcesarEventos.Resultado resultado = procesador(consumidor).ejecutar();

    assertThat(consumidor.recibidos).hasSize(25);
    assertThat(resultado.hechos()).isEqualTo(25);
  }

  @Test
  void reabrirBorraLosFragmentosDelTicket() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    BaseConocimientoEnMemoria base = new BaseConocimientoEnMemoria();
    IndexarConocimiento indexar =
        new IndexarConocimiento(new EmbebedorFalso(), base, new ArticulosEnMemoria(), tickets);
    Ticket ticket = Banco.abrir(tickets, acme, autor, Prioridad.ALTA, AHORA);
    ticket.transicionar(EstadoTicket.RESUELTO, Rol.AGENTE);
    indexar.ticket(ticket);
    assertThat(base.fragmentosDe(ticket.id().valor())).hasSize(1);
    Ticket otro = Banco.abrir(tickets, acme, autor, Prioridad.ALTA, AHORA);
    otro.transicionar(EstadoTicket.RESUELTO, Rol.AGENTE);
    indexar.ticket(otro);
    ticket.transicionar(EstadoTicket.ABIERTO, Rol.AGENTE);
    EventoTicket evento = EventoTicket.nuevo(TipoEvento.TICKET_REABIERTO, acme, ticket.id(), AHORA);
    bandeja.agregar(evento);

    procesador(new DesindexarTicketReabierto(base, tickets)).ejecutar();

    assertThat(base.fragmentosDe(ticket.id().valor())).isEmpty();
    assertThat(base.fragmentosDe(otro.id().valor())).hasSize(1);
    assertThat(bandeja.estadoDe(evento.id())).isEqualTo(Estado.HECHO);
  }

  @Test
  void reabrirSoloBorraLosFragmentosDeLaOrganizacionDelEvento() {
    BaseConocimientoEnMemoria base = new BaseConocimientoEnMemoria();
    OrganizacionId otraOrg = new OrganizacionId(UUID.randomUUID());
    UUID ticketId = UUID.randomUUID();
    base.sembrarTicket(otraOrg, ticketId, "ajeno", "texto");
    EventoTicket evento =
        EventoTicket.nuevo(
            TipoEvento.TICKET_REABIERTO,
            acme,
            new com.admaal.resolvo.domain.TicketId(ticketId),
            AHORA);
    bandeja.agregar(evento);

    procesador(new DesindexarTicketReabierto(base, new RepositorioEnMemoria())).ejecutar();

    assertThat(base.fragmentosDe(ticketId)).hasSize(1);
  }

  @Test
  void elIndexadorIgnoraUnTicketQueYaNoExisteOYaNoEstaResuelto() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    BaseConocimientoEnMemoria base = new BaseConocimientoEnMemoria();
    IndexarConocimiento indexar =
        new IndexarConocimiento(new EmbebedorFalso(), base, new ArticulosEnMemoria(), tickets);
    Ticket reabierto = Banco.abrir(tickets, acme, autor, Prioridad.ALTA, AHORA);
    EventoTicket sobreReabierto =
        EventoTicket.nuevo(TipoEvento.TICKET_RESUELTO, acme, reabierto.id(), AHORA);
    EventoTicket sobreInexistente =
        EventoTicket.nuevo(
            TipoEvento.TICKET_RESUELTO,
            acme,
            new com.admaal.resolvo.domain.TicketId(UUID.randomUUID()),
            AHORA);
    bandeja.agregar(sobreReabierto);
    bandeja.agregar(sobreInexistente);

    procesador(new IndexarTicketResuelto(indexar, tickets)).ejecutar();

    assertThat(base.total()).isZero();
    assertThat(bandeja.estadoDe(sobreReabierto.id())).isEqualTo(Estado.HECHO);
    assertThat(bandeja.estadoDe(sobreInexistente.id())).isEqualTo(Estado.HECHO);
  }

  @Test
  void elReintentoDeUnaReaperturaNoBorraUnTicketQueYaSeVolvioAResolver() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    BaseConocimientoEnMemoria base = new BaseConocimientoEnMemoria();
    IndexarConocimiento indexar =
        new IndexarConocimiento(new EmbebedorFalso(), base, new ArticulosEnMemoria(), tickets);
    Ticket ticket = Banco.abrir(tickets, acme, autor, Prioridad.ALTA, AHORA);
    ticket.transicionar(EstadoTicket.RESUELTO, Rol.AGENTE);
    ticket.transicionar(EstadoTicket.ABIERTO, Rol.AGENTE);
    // la reapertura falla al borrar y, antes de su reintento, el ticket se vuelve a resolver
    ticket.transicionar(EstadoTicket.RESUELTO, Rol.AGENTE);
    indexar.ticket(ticket);
    EventoTicket reapertura =
        EventoTicket.nuevo(TipoEvento.TICKET_REABIERTO, acme, ticket.id(), AHORA);
    bandeja.agregar(reapertura);

    procesador(new DesindexarTicketReabierto(base, tickets)).ejecutar();

    assertThat(base.fragmentosDe(ticket.id().valor())).hasSize(1);
    assertThat(bandeja.estadoDe(reapertura.id())).isEqualTo(Estado.HECHO);
  }

  @Test
  void losConsumidoresDeIndexadoTienenNombreYTipoPropios() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    BaseConocimientoEnMemoria base = new BaseConocimientoEnMemoria();
    IndexarConocimiento indexar =
        new IndexarConocimiento(new EmbebedorFalso(), base, new ArticulosEnMemoria(), tickets);
    IndexarTicketResuelto indexa = new IndexarTicketResuelto(indexar, tickets);
    DesindexarTicketReabierto desindexa = new DesindexarTicketReabierto(base, tickets);

    assertThat(indexa.nombre()).isEqualTo("indexar-ticket");
    assertThat(indexa.admite(TipoEvento.TICKET_RESUELTO)).isTrue();
    assertThat(indexa.admite(TipoEvento.TICKET_REABIERTO)).isFalse();
    assertThat(desindexa.nombre()).isEqualTo("desindexar-ticket");
    assertThat(desindexa.admite(TipoEvento.TICKET_REABIERTO)).isTrue();
    assertThat(desindexa.admite(TipoEvento.TICKET_RESUELTO)).isFalse();
  }
}
