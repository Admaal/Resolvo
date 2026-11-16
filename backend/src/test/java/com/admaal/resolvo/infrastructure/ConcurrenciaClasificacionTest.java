package com.admaal.resolvo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.application.AbrirTicketComando;
import com.admaal.resolvo.application.ClasificacionPropuesta;
import com.admaal.resolvo.application.ClasificadorTickets;
import com.admaal.resolvo.application.Embebedor;
import com.admaal.resolvo.application.RedactorRespuestas;
import com.admaal.resolvo.application.soporte.EmbebedorFalso;
import com.admaal.resolvo.application.soporte.RedactorFalso;
import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TransicionNoPermitida;
import com.admaal.resolvo.domain.UsuarioId;
import com.admaal.resolvo.infrastructure.AplicacionDeClasificacion.ClasificacionVista;
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

/** Doble clic desde dos pestanas, o dos agentes sobre la misma propuesta, a la vez. */
@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
@Import(ConcurrenciaClasificacionTest.Ia.class)
class ConcurrenciaClasificacionTest {

  private static final OrganizacionId ACME =
      new OrganizacionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  private static final UsuarioId AGENTE =
      new UsuarioId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
  private static final CategoriaId INCIDENCIA =
      new CategoriaId(UUID.fromString("33333333-3333-3333-3333-333333333331"));

  /** Hace esperar a las dos peticiones dentro del proveedor para que se solapen de verdad. */
  static class ClasificadorConBarrera implements ClasificadorTickets {
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
    public ClasificacionPropuesta clasificar(Ticket ticket, List<Categoria> categorias) {
      llamadas.incrementAndGet();
      try {
        barrera.await(3, TimeUnit.SECONDS);
      } catch (InterruptedException interrumpido) {
        Thread.currentThread().interrupt();
      } catch (Exception sinPareja) {
        // La otra peticion no llego: seguimos para que el test no se quede colgado.
      }
      return new ClasificacionPropuesta("Consulta", Prioridad.URGENTE, "motivo");
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
    RedactorRespuestas redactor() {
      return new RedactorFalso();
    }

    @Bean
    @Primary
    ClasificadorConBarrera clasificador() {
      return new ClasificadorConBarrera();
    }
  }

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired AplicacionDeClasificacion clasificacion;
  @Autowired AplicacionDeResolvo resolvo;
  @Autowired ClasificadorConBarrera clasificador;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void preparar() {
    jdbc.update("delete from clasificacion");
    clasificador.reiniciar();
  }

  private Ticket ticket() {
    return resolvo.abrir(
        new AbrirTicketComando(
            ACME, AGENTE, Rol.AGENTE, INCIDENCIA, Prioridad.BAJA, "Carrera", "Dos peticiones a la vez"));
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
  void dobleClicNoDuplica() throws Exception {
    Ticket ticket = ticket();
    ExecutorService hilos = Executors.newFixedThreadPool(2);
    try {
      List<Future<ClasificacionVista>> trabajos =
          enParalelo(hilos, 2, () -> clasificacion.sugerir(ACME, ticket.id(), AGENTE, Rol.AGENTE));

      ClasificacionVista primera = trabajos.get(0).get(30, TimeUnit.SECONDS);
      ClasificacionVista segunda = trabajos.get(1).get(30, TimeUnit.SECONDS);

      assertThat(primera.clasificacion().id()).isEqualTo(segunda.clasificacion().id());
      assertThat(List.of(primera, segunda).stream().filter(ClasificacionVista::creada).count())
          .isEqualTo(1);
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from clasificacion where ticket_id = ?::uuid",
                  Integer.class,
                  ticket.id().valor().toString()))
          .isEqualTo(1);
    } finally {
      hilos.shutdownNow();
    }
  }

  @Test
  void dosAplicacionesSimultaneasAplicanUnaSolaVez() throws Exception {
    ExecutorService hilos = Executors.newFixedThreadPool(2);
    try {
      for (int ronda = 0; ronda < 10; ronda++) {
        clasificador.reiniciar();
        Ticket ticket = ticket();
        var propuesta = clasificacion.sugerir(ACME, ticket.id(), AGENTE, Rol.AGENTE).clasificacion();

        List<Future<Boolean>> trabajos =
            enParalelo(
                hilos,
                2,
                () -> {
                  try {
                    clasificacion.aplicar(
                        ACME, propuesta.id(), INCIDENCIA, Prioridad.MEDIA, AGENTE, Rol.AGENTE);
                    return true;
                  } catch (TransicionNoPermitida perdedora) {
                    return false;
                  }
                });

        int aplicadas = 0;
        for (Future<Boolean> trabajo : trabajos) {
          try {
            if (trabajo.get(30, TimeUnit.SECONDS)) {
              aplicadas++;
            }
          } catch (ExecutionException inesperado) {
            throw new AssertionError(
                "la perdedora debe recibir un conflicto, no " + inesperado.getCause(), inesperado);
          }
        }

        assertThat(aplicadas).as("ronda %d", ronda).isEqualTo(1);
        assertThat(
                jdbc.queryForObject(
                    "select estado from clasificacion where id = ?::uuid",
                    String.class,
                    propuesta.id().valor().toString()))
            .isEqualTo("APLICADA");
      }
    } finally {
      hilos.shutdownNow();
    }
  }
}
