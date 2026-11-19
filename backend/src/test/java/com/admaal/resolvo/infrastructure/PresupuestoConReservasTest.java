package com.admaal.resolvo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.Presupuesto;
import com.admaal.resolvo.application.PresupuestoAgotado;
import com.admaal.resolvo.application.soporte.RegistroLlamadasEnMemoria;
import com.admaal.resolvo.application.soporte.RelojManual;
import com.admaal.resolvo.domain.LlamadaIa;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.PresupuestoDiario;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PresupuestoConReservasTest {

  private static final Instant MEDIODIA = Instant.parse("2026-11-19T12:00:00Z");

  private final OrganizacionId acme = new OrganizacionId(UUID.randomUUID());
  private final OrganizacionId otra = new OrganizacionId(UUID.randomUUID());
  private final RegistroLlamadasEnMemoria registro = new RegistroLlamadasEnMemoria();
  private final RelojManual reloj = new RelojManual(MEDIODIA);

  private PresupuestoConReservas presupuesto(int llamadas, long tokens) {
    return new PresupuestoConReservas(registro, reloj, new PresupuestoDiario(llamadas, tokens));
  }

  private void gastar(OrganizacionId org, int tokens, Instant cuando) {
    registro.registrar(
        new LlamadaIa(UUID.randomUUID(), org, "sugerir", "m", tokens, 0, 100, true, cuando));
  }

  @Test
  void admiteMientrasQuedeCupoYSeAgotaConLasLlamadasDelDia() {
    PresupuestoConReservas presupuesto = presupuesto(2, 1_000_000);

    presupuesto.reservar(acme).close();
    gastar(acme, 10, MEDIODIA);
    presupuesto.reservar(acme).close();
    gastar(acme, 10, MEDIODIA);

    assertThatThrownBy(() -> presupuesto.reservar(acme)).isInstanceOf(PresupuestoAgotado.class);
  }

  @Test
  void seAgotaConLosTokensDelDia() {
    PresupuestoConReservas presupuesto = presupuesto(100, 500);
    gastar(acme, 499, MEDIODIA);
    presupuesto.reservar(acme).close();
    gastar(acme, 1, MEDIODIA);

    assertThatThrownBy(() -> presupuesto.reservar(acme)).isInstanceOf(PresupuestoAgotado.class);
  }

  @Test
  void elConsumoDeAyerNoCuentaHoy() {
    PresupuestoConReservas presupuesto = presupuesto(1, 1_000);
    gastar(acme, 10, Instant.parse("2026-11-18T23:59:59Z"));

    presupuesto.reservar(acme).close();
  }

  @Test
  void elPresupuestoSeRenuevaAMedianocheUtc() {
    PresupuestoConReservas presupuesto = presupuesto(1, 1_000);
    reloj.avanzar(Duration.ofHours(11).plusMinutes(59).plusSeconds(59));
    gastar(acme, 10, reloj.ahora());
    assertThatThrownBy(() -> presupuesto.reservar(acme)).isInstanceOf(PresupuestoAgotado.class);

    reloj.avanzar(Duration.ofSeconds(1));

    presupuesto.reservar(acme).close();
  }

  @Test
  void elConsumoDeOtraOrganizacionNoCuenta() {
    PresupuestoConReservas presupuesto = presupuesto(1, 1_000);
    gastar(otra, 10, MEDIODIA);
    gastar(otra, 10, MEDIODIA);

    presupuesto.reservar(acme).close();
    assertThatThrownBy(() -> presupuesto.reservar(otra)).isInstanceOf(PresupuestoAgotado.class);
  }

  @Test
  void unaReservaAbiertaOcupaElCupoHastaQueSeCierra() {
    PresupuestoConReservas presupuesto = presupuesto(1, 1_000);

    Presupuesto.Reserva primera = presupuesto.reservar(acme);
    assertThatThrownBy(() -> presupuesto.reservar(acme)).isInstanceOf(PresupuestoAgotado.class);
    primera.close();

    presupuesto.reservar(acme).close();
  }

  @Test
  void lasReservasDeUnaOrganizacionNoOcupanElCupoDeOtra() {
    PresupuestoConReservas presupuesto = presupuesto(1, 1_000);

    Presupuesto.Reserva deAcme = presupuesto.reservar(acme);

    presupuesto.reservar(otra).close();
    deAcme.close();
  }

  @Test
  void cerrarDosVecesUnaReservaLiberaElCupoSoloUnaVez() {
    PresupuestoConReservas presupuesto = presupuesto(2, 1_000);
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
    PresupuestoConReservas presupuesto = presupuesto(1, 1_000);
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
}
