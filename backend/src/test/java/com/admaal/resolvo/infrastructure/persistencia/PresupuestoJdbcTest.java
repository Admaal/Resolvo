package com.admaal.resolvo.infrastructure.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.Presupuesto;
import com.admaal.resolvo.application.PresupuestoAgotado;
import com.admaal.resolvo.application.RegistroLlamadas;
import com.admaal.resolvo.application.soporte.RegistroLlamadasEnMemoria;
import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.domain.Consumo;
import com.admaal.resolvo.application.soporte.RelojManual;
import com.admaal.resolvo.domain.LlamadaIa;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.PresupuestoDiario;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * El presupuesto guarda sus reservas en Postgres con un cerrojo por organizacion: vale con varias
 * instancias y una organizacion no espera a otra. El consumo ya registrado sale de un registro en
 * memoria para que cada test controle lo gastado.
 */
@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
class PresupuestoJdbcTest {

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate transacciones;

  private static final Instant MEDIODIA = Instant.parse("2026-11-19T12:00:00Z");

  private final OrganizacionId acme = new OrganizacionId(UUID.randomUUID());
  private final OrganizacionId otra = new OrganizacionId(UUID.randomUUID());
  private final RegistroLlamadasEnMemoria registro = new RegistroLlamadasEnMemoria();
  private final RelojManual reloj = new RelojManual(MEDIODIA);

  @BeforeEach
  void limpiar() {
    jdbc.update("delete from presupuesto_reserva");
  }

  private PresupuestoJdbc presupuesto(int llamadas, long tokens) {
    return presupuesto(registro, llamadas, tokens);
  }

  private PresupuestoJdbc presupuesto(
      RegistroLlamadas registro, int llamadas, long tokens) {
    return new PresupuestoJdbc(
        jdbc, transacciones, registro, reloj, new PresupuestoDiario(llamadas, tokens));
  }

  private void gastar(OrganizacionId org, int tokens, Instant cuando) {
    registro.registrar(
        new LlamadaIa(UUID.randomUUID(), org, "sugerir", "m", tokens, 0, 100, true, cuando));
  }

  @Test
  void admiteMientrasQuedeCupoYSeAgotaConLasLlamadasDelDia() {
    PresupuestoJdbc presupuesto = presupuesto(2, 1_000_000);

    presupuesto.reservar(acme).close();
    gastar(acme, 10, MEDIODIA);
    presupuesto.reservar(acme).close();
    gastar(acme, 10, MEDIODIA);

    assertThatThrownBy(() -> presupuesto.reservar(acme)).isInstanceOf(PresupuestoAgotado.class);
  }

  @Test
  void seAgotaConLosTokensDelDia() {
    PresupuestoJdbc presupuesto = presupuesto(100, 500);
    gastar(acme, 499, MEDIODIA);
    presupuesto.reservar(acme).close();
    gastar(acme, 1, MEDIODIA);

    assertThatThrownBy(() -> presupuesto.reservar(acme)).isInstanceOf(PresupuestoAgotado.class);
  }

  @Test
  void elConsumoDeAyerNoCuentaHoy() {
    PresupuestoJdbc presupuesto = presupuesto(1, 1_000);
    gastar(acme, 10, Instant.parse("2026-11-18T23:59:59Z"));

    presupuesto.reservar(acme).close();
  }

  @Test
  void elPresupuestoSeRenuevaAMedianocheUtc() {
    PresupuestoJdbc presupuesto = presupuesto(1, 1_000);
    reloj.avanzar(Duration.ofHours(11).plusMinutes(59).plusSeconds(59));
    gastar(acme, 10, reloj.ahora());
    assertThatThrownBy(() -> presupuesto.reservar(acme)).isInstanceOf(PresupuestoAgotado.class);

    reloj.avanzar(Duration.ofSeconds(1));

    presupuesto.reservar(acme).close();
  }

  @Test
  void elConsumoDeOtraOrganizacionNoCuenta() {
    PresupuestoJdbc presupuesto = presupuesto(1, 1_000);
    gastar(otra, 10, MEDIODIA);
    gastar(otra, 10, MEDIODIA);

    presupuesto.reservar(acme).close();
    assertThatThrownBy(() -> presupuesto.reservar(otra)).isInstanceOf(PresupuestoAgotado.class);
  }

  @Test
  void unaReservaAbiertaOcupaElCupoHastaQueSeCierra() {
    PresupuestoJdbc presupuesto = presupuesto(1, 1_000);

    Presupuesto.Reserva primera = presupuesto.reservar(acme);
    assertThatThrownBy(() -> presupuesto.reservar(acme)).isInstanceOf(PresupuestoAgotado.class);
    primera.close();

    presupuesto.reservar(acme).close();
  }

  @Test
  void lasReservasDeUnaOrganizacionNoOcupanElCupoDeOtra() {
    PresupuestoJdbc presupuesto = presupuesto(1, 1_000);

    Presupuesto.Reserva deAcme = presupuesto.reservar(acme);

    presupuesto.reservar(otra).close();
    deAcme.close();
  }

  @Test
  void cerrarDosVecesUnaReservaLiberaElCupoSoloUnaVez() {
    PresupuestoJdbc presupuesto = presupuesto(2, 1_000);
    Presupuesto.Reserva primera = presupuesto.reservar(acme);
    presupuesto.reservar(acme);

    primera.close();
    primera.close();

    // Queda abierta la segunda: esta tercera llena el cupo y una cuarta ya no cabe.
    presupuesto.reservar(acme);
    assertThatThrownBy(() -> presupuesto.reservar(acme)).isInstanceOf(PresupuestoAgotado.class);
  }

  @Test
  void dosReservasSimultaneasConUnaLlamadaLibreSoloAdmitenUna() throws Exception {
    PresupuestoJdbc presupuesto = presupuesto(1, 1_000);
    int hilos = 20;
    ExecutorService pool = Executors.newFixedThreadPool(hilos);
    CountDownLatch salida = new CountDownLatch(1);
    AtomicInteger admitidas = new AtomicInteger();
    List<Future<?>> trabajos = new ArrayList<>();
    for (int i = 0; i < hilos; i++) {
      trabajos.add(
          pool.submit(
              () -> {
                salida.await();
                try {
                  presupuesto.reservar(acme);
                  admitidas.incrementAndGet();
                } catch (PresupuestoAgotado agotado) {
                  // Esperado para todas menos una.
                }
                return null;
              }));
    }
    salida.countDown();
    for (Future<?> trabajo : trabajos) {
      trabajo.get();
    }
    pool.shutdown();

    assertThat(admitidas.get()).isEqualTo(1);
  }

  @Test
  void dosInstanciasNoPasanJuntasDelLimite() {
    PresupuestoJdbc una = presupuesto(1, 1_000);
    PresupuestoJdbc otraInstancia = presupuesto(1, 1_000);

    Presupuesto.Reserva abierta = una.reservar(acme);

    assertThatThrownBy(() -> otraInstancia.reservar(acme)).isInstanceOf(PresupuestoAgotado.class);
    abierta.close();
    otraInstancia.reservar(acme).close();
  }

  @Test
  void unaOrganizacionQueEsperaASuConsultaNoFrenaAOtra() throws Exception {
    CountDownLatch dentro = new CountDownLatch(1);
    CountDownLatch soltar = new CountDownLatch(1);
    RegistroLlamadas lento =
        new RegistroLlamadas() {
          @Override
          public void registrar(LlamadaIa llamada) {
            registro.registrar(llamada);
          }

          @Override
          public List<LlamadaIa> deLaOrganizacion(
              OrganizacionId organizacionId, Instant desde, Instant hasta) {
            return registro.deLaOrganizacion(organizacionId, desde, hasta);
          }

          @Override
          public Consumo consumoDe(OrganizacionId organizacionId, Instant desde, Instant hasta) {
            if (organizacionId.equals(acme)) {
              dentro.countDown();
              try {
                soltar.await(10, TimeUnit.SECONDS);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            }
            return registro.consumoDe(organizacionId, desde, hasta);
          }
        };
    PresupuestoJdbc presupuesto = presupuesto(lento, 5, 1_000);
    ExecutorService pool = Executors.newSingleThreadExecutor();
    Future<?> deAcme = pool.submit(() -> presupuesto.reservar(acme).close());
    assertThat(dentro.await(5, TimeUnit.SECONDS)).isTrue();

    long inicio = System.nanoTime();
    presupuesto.reservar(otra).close();
    long milisegundos = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);

    soltar.countDown();
    deAcme.get(10, TimeUnit.SECONDS);
    pool.shutdown();
    assertThat(milisegundos).isLessThan(2_000);
  }

  @Test
  void unaReservaQueNadieCierraCaducaSola() {
    PresupuestoJdbc presupuesto = presupuesto(1, 1_000);
    presupuesto.reservar(acme);
    assertThatThrownBy(() -> presupuesto.reservar(acme)).isInstanceOf(PresupuestoAgotado.class);

    reloj.avanzar(PresupuestoJdbc.CADUCIDAD_DE_RESERVA.plusSeconds(1));

    presupuesto.reservar(acme).close();
  }
}
