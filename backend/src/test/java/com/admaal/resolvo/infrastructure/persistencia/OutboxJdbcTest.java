package com.admaal.resolvo.infrastructure.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.application.BandejaEventos.Reclamado;
import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.domain.EventoTicket;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.TipoEvento;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
class OutboxJdbcTest {

  private static final OrganizacionId ACME =
      new OrganizacionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired OutboxJdbc outbox;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager transacciones;

  @BeforeEach
  void limpiar() {
    jdbc.update("delete from evento_procesado");
    jdbc.update("delete from evento_outbox");
  }

  private EventoTicket evento(Instant cuando) {
    return EventoTicket.nuevo(
        TipoEvento.TICKET_RESUELTO, ACME, new TicketId(UUID.randomUUID()), cuando);
  }

  private String estado(UUID id) {
    return jdbc.queryForObject("select estado from evento_outbox where id = ?", String.class, id);
  }

  @Test
  void publicarGuardaElEventoConTodosSusDatos() {
    EventoTicket evento = evento(Instant.parse("2026-11-19T10:00:00Z"));

    outbox.publicar(evento);

    List<Reclamado> reclamados = outbox.reclamar(10);
    assertThat(reclamados).singleElement().satisfies(r -> {
      assertThat(r.evento()).isEqualTo(evento);
      assertThat(r.intentos()).isEqualTo(1);
    });
  }

  @Test
  void elEventoDeComentarioModeradoSeGuardaYSeLeeConSuTipo() {
    EventoTicket evento =
        EventoTicket.nuevo(
            TipoEvento.COMENTARIO_MODERADO,
            ACME,
            new TicketId(UUID.randomUUID()),
            Instant.parse("2026-11-21T10:00:00Z"));

    outbox.publicar(evento);

    assertThat(outbox.reclamar(10)).singleElement().satisfies(r -> assertThat(r.evento()).isEqualTo(evento));
  }

  @Test
  void siLaTransaccionSeRevierteNoQuedaEvento() {
    EventoTicket evento = evento(Instant.now());

    new TransactionTemplate(transacciones)
        .execute(
            estado -> {
              outbox.publicar(evento);
              estado.setRollbackOnly();
              return null;
            });

    assertThat(outbox.pendientes()).isZero();
  }

  @Test
  void siLaTransaccionSeConfirmaElEventoQueda() {
    EventoTicket evento = evento(Instant.now());

    new TransactionTemplate(transacciones).executeWithoutResult(estado -> outbox.publicar(evento));

    assertThat(outbox.pendientes()).isEqualTo(1);
  }

  @Test
  void reclamarDevuelveLosEventosEnOrdenYUnoReclamadoNoVuelveASalir() {
    EventoTicket tarde = evento(Instant.parse("2026-11-19T10:00:05Z"));
    EventoTicket pronto = evento(Instant.parse("2026-11-19T10:00:01Z"));
    outbox.publicar(tarde);
    outbox.publicar(pronto);

    List<Reclamado> primero = outbox.reclamar(10);
    List<Reclamado> segundo = outbox.reclamar(10);

    assertThat(primero).extracting(r -> r.evento().id()).containsExactly(pronto.id(), tarde.id());
    assertThat(segundo).isEmpty();
  }

  @Test
  void reclamarRespetaElMaximo() {
    for (int i = 0; i < 5; i++) {
      outbox.publicar(evento(Instant.now().plusSeconds(i)));
    }

    assertThat(outbox.reclamar(2)).hasSize(2);
    assertThat(outbox.reclamar(10)).hasSize(3);
  }

  @Test
  void alVencerElReclamoElEventoVuelveConUnIntentoMas() {
    EventoTicket evento = evento(Instant.now());
    outbox.publicar(evento);
    outbox.reclamar(10);
    jdbc.update(
        "update evento_outbox set reclamado_hasta = now() - interval '1 second' where id = ?",
        evento.id());

    List<Reclamado> otraVez = outbox.reclamar(10);

    assertThat(otraVez).singleElement().satisfies(r -> assertThat(r.intentos()).isEqualTo(2));
  }

  @Test
  void unEventoHechoNoSeVuelveAReclamar() {
    EventoTicket evento = evento(Instant.now());
    outbox.publicar(evento);
    outbox.reclamar(10);

    outbox.marcarHecho(evento.id());
    jdbc.update("update evento_outbox set reclamado_hasta = null where id = ?", evento.id());

    assertThat(estado(evento.id())).isEqualTo("HECHO");
    assertThat(outbox.reclamar(10)).isEmpty();
    assertThat(outbox.pendientes()).isZero();
  }

  @Test
  void fallarAnotaElErrorYSoloUnFalloDefinitivoLoDaPorFallido() {
    EventoTicket evento = evento(Instant.now());
    outbox.publicar(evento);
    outbox.reclamar(10);

    outbox.fallar(evento.id(), "x".repeat(900), false);

    assertThat(estado(evento.id())).isEqualTo("PENDIENTE");
    assertThat(
            jdbc.queryForObject(
                "select length(ultimo_error) from evento_outbox where id = ?",
                Integer.class,
                evento.id()))
        .isEqualTo(500);

    outbox.fallar(evento.id(), "boom", true);

    assertThat(estado(evento.id())).isEqualTo("FALLIDO");
    jdbc.update("update evento_outbox set reclamado_hasta = null where id = ?", evento.id());
    assertThat(outbox.reclamar(10)).isEmpty();
  }

  @Test
  void unFalloNoDefinitivoAcortaElReclamoParaQueElSiguienteBarridoLoReintente() {
    EventoTicket evento = evento(Instant.now());
    outbox.publicar(evento);
    outbox.reclamar(10);

    outbox.fallar(evento.id(), "boom", false);

    // el barrido corre cada minuto: el reintento tiene que quedar antes que el siguiente barrido
    assertThat(
            jdbc.queryForObject(
                "select reclamado_hasta < now() + interval '40 seconds' from evento_outbox where id = ?",
                Boolean.class,
                evento.id()))
        .isTrue();
    assertThat(outbox.pendientes()).isEqualTo(1);
  }

  @Test
  void registrarUnConsumidorDosVecesEsInofensivoYSeDistinguePorConsumidor() {
    UUID id = UUID.randomUUID();
    assertThat(outbox.yaProcesado("indexar-ticket", id)).isFalse();

    outbox.registrar("indexar-ticket", id);
    outbox.registrar("indexar-ticket", id);

    assertThat(outbox.yaProcesado("indexar-ticket", id)).isTrue();
    assertThat(outbox.yaProcesado("desindexar-ticket", id)).isFalse();
    assertThat(outbox.yaProcesado("indexar-ticket", UUID.randomUUID())).isFalse();
  }

  @Test
  void dosProcesadoresALaVezNuncaReclamanElMismoEvento() throws Exception {
    Set<UUID> publicados = new HashSet<>();
    for (int i = 0; i < 40; i++) {
      EventoTicket evento = evento(Instant.now().plusMillis(i));
      outbox.publicar(evento);
      publicados.add(evento.id());
    }
    int hilos = 4;
    ExecutorService pool = Executors.newFixedThreadPool(hilos);
    CountDownLatch salida = new CountDownLatch(1);
    List<Future<List<Reclamado>>> tareas = new ArrayList<>();
    for (int i = 0; i < hilos; i++) {
      tareas.add(
          pool.submit(
              () -> {
                salida.await();
                return outbox.reclamar(10);
              }));
    }
    salida.countDown();

    List<UUID> vistos = new ArrayList<>();
    for (Future<List<Reclamado>> tarea : tareas) {
      tarea.get().forEach(r -> vistos.add(r.evento().id()));
    }
    pool.shutdown();

    assertThat(vistos).doesNotHaveDuplicates();
    assertThat(publicados).containsAll(vistos);
    assertThat(vistos).hasSizeLessThanOrEqualTo(40);
    // lo que no reclamo nadie sigue disponible: entre todos se agota la bandeja sin repetir
    List<UUID> resto = new ArrayList<>();
    outbox.reclamar(100).forEach(r -> resto.add(r.evento().id()));
    assertThat(resto).doesNotContainAnyElementsOf(vistos);
    assertThat(vistos.size() + resto.size()).isEqualTo(40);
  }
}
