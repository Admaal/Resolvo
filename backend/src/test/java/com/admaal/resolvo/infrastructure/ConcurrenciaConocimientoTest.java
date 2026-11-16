package com.admaal.resolvo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.application.AbrirTicketComando;
import com.admaal.resolvo.application.BorradorConCitas;
import com.admaal.resolvo.application.Embebedor;
import com.admaal.resolvo.application.RedactorRespuestas;
import com.admaal.resolvo.application.soporte.EmbebedorFalso;
import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TransicionNoPermitida;
import com.admaal.resolvo.domain.UsuarioId;
import com.admaal.resolvo.infrastructure.AplicacionDeConocimiento.SugerenciaVista;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Peticiones que llegan a la vez: doble clic desde dos pestanas o dos agentes sobre la misma sugerencia. */
@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
@Import(ConcurrenciaConocimientoTest.Ia.class)
class ConcurrenciaConocimientoTest {

  private static final OrganizacionId ACME =
      new OrganizacionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  private static final UsuarioId AGENTE =
      new UsuarioId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
  private static final CategoriaId INCIDENCIA =
      new CategoriaId(UUID.fromString("33333333-3333-3333-3333-333333333331"));

  /** Hace esperar a las dos peticiones dentro del proveedor para que se solapen de verdad. */
  static class RedactorConBarrera implements RedactorRespuestas {
    private volatile CyclicBarrier barrera = new CyclicBarrier(2);
    final AtomicInteger llamadas = new AtomicInteger();

    void reiniciar() {
      barrera = new CyclicBarrier(2);
      llamadas.set(0);
    }

    @Override
    public String modelo() {
      return "barrera";
    }

    @Override
    public BorradorConCitas redactar(Ticket ticket, List<Fragmento> fragmentos) {
      llamadas.incrementAndGet();
      try {
        barrera.await(3, TimeUnit.SECONDS);
      } catch (InterruptedException interrumpido) {
        Thread.currentThread().interrupt();
      } catch (Exception sinPareja) {
        // La otra peticion no llego: seguimos para que el test no se quede colgado.
      }
      return new BorradorConCitas("Borrador", List.of(fragmentos.get(0).id()));
    }
  }

  @TestConfiguration
  static class Ia {
    @Bean
    @Primary
    Embebedor embebedor() {
      return new EmbebedorFalso();
    }

    @Bean
    @Primary
    RedactorConBarrera redactor() {
      return new RedactorConBarrera();
    }
  }

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired AplicacionDeConocimiento conocimiento;
  @Autowired AplicacionDeResolvo resolvo;
  @Autowired RedactorConBarrera redactor;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void preparar() {
    jdbc.update("delete from cita");
    jdbc.update("delete from sugerencia");
    redactor.reiniciar();
    conocimiento.reindexar(ACME, Rol.ADMIN);
  }

  private Ticket ticket() {
    return resolvo.abrir(
        new AbrirTicketComando(
            ACME, AGENTE, Rol.AGENTE, INCIDENCIA, Prioridad.ALTA, "Carrera", "Dos peticiones a la vez"));
  }

  private <T> List<Future<T>> enParalelo(ExecutorService hilos, int veces, Callable<T> tarea) {
    CountDownLatch salida = new CountDownLatch(1);
    List<Future<T>> trabajos = new ArrayList<>();
    for (int i = 0; i < veces; i++) {
      trabajos.add(
          hilos.submit(
              () -> {
                salida.await();
                return tarea.call();
              }));
    }
    salida.countDown();
    return trabajos;
  }

  @Test
  void dosSugerirSimultaneosDevuelvenLaMismaSugerenciaYNoDuplican() throws Exception {
    Ticket ticket = ticket();
    ExecutorService hilos = Executors.newFixedThreadPool(2);
    try {
      List<Future<SugerenciaVista>> trabajos =
          enParalelo(hilos, 2, () -> conocimiento.sugerir(ACME, ticket.id(), AGENTE, Rol.AGENTE));

      SugerenciaVista primera = trabajos.get(0).get(30, TimeUnit.SECONDS);
      SugerenciaVista segunda = trabajos.get(1).get(30, TimeUnit.SECONDS);

      assertThat(primera.sugerencia().id()).isEqualTo(segunda.sugerencia().id());
      assertThat(List.of(primera, segunda).stream().filter(SugerenciaVista::creada).count()).isEqualTo(1);
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from sugerencia where ticket_id = ?::uuid",
                  Integer.class,
                  ticket.id().valor().toString()))
          .isEqualTo(1);
    } finally {
      hilos.shutdownNow();
    }
  }

  @Test
  void dosAprobacionesSimultaneasPublicanUnSoloComentario() throws Exception {
    ExecutorService hilos = Executors.newFixedThreadPool(2);
    try {
      for (int ronda = 0; ronda < 10; ronda++) {
        redactor.reiniciar();
        Ticket ticket = ticket();
        var sugerencia = conocimiento.sugerir(ACME, ticket.id(), AGENTE, Rol.AGENTE).sugerencia();
        int numero = ronda;

        List<Future<Boolean>> trabajos =
            enParalelo(
                hilos,
                2,
                () -> {
                  try {
                    conocimiento.aprobar(ACME, sugerencia.id(), AGENTE, Rol.AGENTE, "Final " + numero);
                    return true;
                  } catch (TransicionNoPermitida perdedora) {
                    return false;
                  }
                });

        int aprobadas = 0;
        for (Future<Boolean> trabajo : trabajos) {
          try {
            if (trabajo.get(30, TimeUnit.SECONDS)) {
              aprobadas++;
            }
          } catch (ExecutionException inesperado) {
            throw new AssertionError("la perdedora debe recibir un conflicto, no " + inesperado.getCause(), inesperado);
          }
        }

        assertThat(aprobadas).as("ronda %d", ronda).isEqualTo(1);
        assertThat(
                jdbc.queryForObject(
                    "select count(*) from comentario where ticket_id = ?::uuid",
                    Integer.class,
                    ticket.id().valor().toString()))
            .as("comentarios de la ronda %d", ronda)
            .isEqualTo(1);
      }
    } finally {
      hilos.shutdownNow();
    }
  }
}
