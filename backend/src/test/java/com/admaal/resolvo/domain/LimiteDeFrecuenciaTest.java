package com.admaal.resolvo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LimiteDeFrecuenciaTest {

  private static final Instant T0 = Instant.parse("2026-11-19T10:00:00Z");
  private final LimiteDeFrecuencia limite = new LimiteDeFrecuencia(3, Duration.ofMinutes(1));

  @Test
  void admiteHastaElMaximo() {
    assertThat(limite.admitir("a", T0).admitida()).isTrue();
    assertThat(limite.admitir("a", T0.plusSeconds(1)).admitida()).isTrue();
    assertThat(limite.admitir("a", T0.plusSeconds(2)).admitida()).isTrue();
  }

  @Test
  void elSiguienteSeRechazaConSuReintento() {
    limite.admitir("a", T0);
    limite.admitir("a", T0.plusSeconds(10));
    limite.admitir("a", T0.plusSeconds(20));

    Limite.Decision decision = limite.admitir("a", T0.plusSeconds(30));

    assertThat(decision.admitida()).isFalse();
    // La mas antigua (T0) sale de la ventana a los 60 s: faltan 30.
    assertThat(decision.reintentarEn()).isEqualTo(Duration.ofSeconds(30));
  }

  @Test
  void unRechazoNoConsumeCupo() {
    limite.admitir("a", T0);
    limite.admitir("a", T0);
    limite.admitir("a", T0);
    limite.admitir("a", T0.plusSeconds(1));
    limite.admitir("a", T0.plusSeconds(2));

    assertThat(limite.admitir("a", T0.plusSeconds(60)).admitida()).isTrue();
  }

  @Test
  void laVentanaSeDesliza() {
    limite.admitir("a", T0);
    limite.admitir("a", T0.plusSeconds(30));
    limite.admitir("a", T0.plusSeconds(40));
    assertThat(limite.admitir("a", T0.plusSeconds(59)).admitida()).isFalse();

    assertThat(limite.admitir("a", T0.plusSeconds(60)).admitida()).isTrue();
    assertThat(limite.admitir("a", T0.plusSeconds(61)).admitida()).isFalse();
  }

  @Test
  void lasClavesNoSeMezclan() {
    limite.admitir("a", T0);
    limite.admitir("a", T0);
    limite.admitir("a", T0);

    assertThat(limite.admitir("a", T0).admitida()).isFalse();
    assertThat(limite.admitir("b", T0).admitida()).isTrue();
  }

  @Test
  void comprobarNoRegistraYRegistrarSi() {
    assertThat(limite.comprobar("a", T0).admitida()).isTrue();
    assertThat(limite.comprobar("a", T0).admitida()).isTrue();
    assertThat(limite.comprobar("a", T0).admitida()).isTrue();
    limite.registrar("a", T0);
    limite.registrar("a", T0.plusSeconds(1));
    assertThat(limite.comprobar("a", T0.plusSeconds(2)).admitida()).isTrue();
    limite.registrar("a", T0.plusSeconds(2));

    Limite.Decision decision = limite.comprobar("a", T0.plusSeconds(3));

    assertThat(decision.admitida()).isFalse();
    assertThat(decision.reintentarEn()).isEqualTo(Duration.ofSeconds(57));
  }

  @Test
  void reiniciarOlvidaLaClave() {
    limite.registrar("a", T0);
    limite.registrar("a", T0);
    limite.registrar("a", T0);

    limite.reiniciar("a");

    assertThat(limite.comprobar("a", T0).admitida()).isTrue();
  }

  @Test
  void laMemoriaSePurgaCuandoCaducanLasClaves() {
    for (int i = 0; i < 50; i++) {
      limite.admitir("k" + i, T0);
    }
    assertThat(limite.clavesRecordadas()).isEqualTo(50);

    limite.purgar(T0.plusSeconds(61));

    assertThat(limite.clavesRecordadas()).isZero();
  }

  @Test
  void laPurgaConservaLasClavesVivas() {
    limite.admitir("vieja", T0);
    limite.admitir("viva", T0.plusSeconds(50));

    limite.purgar(T0.plusSeconds(61));

    assertThat(limite.clavesRecordadas()).isEqualTo(1);
    assertThat(limite.comprobar("viva", T0.plusSeconds(61)).admitida()).isTrue();
  }

  @Test
  void admisionesConcurrentesNoPasanDelMaximo() throws Exception {
    int hilos = 20;
    ExecutorService pool = Executors.newFixedThreadPool(hilos);
    CountDownLatch salida = new CountDownLatch(1);
    AtomicInteger admitidas = new AtomicInteger();
    List<Future<?>> trabajos = new ArrayList<>();
    for (int i = 0; i < hilos; i++) {
      Callable<Object> trabajo =
          () -> {
            salida.await();
            if (limite.admitir("a", T0).admitida()) {
              admitidas.incrementAndGet();
            }
            return null;
          };
      trabajos.add(pool.submit(trabajo));
    }
    salida.countDown();
    for (Future<?> trabajo : trabajos) {
      trabajo.get();
    }
    pool.shutdown();

    assertThat(admitidas.get()).isEqualTo(3);
  }

  @Test
  void unMaximoOUnaVentanaInvalidosSeRechazan() {
    assertThatThrownBy(() -> new LimiteDeFrecuencia(0, Duration.ofMinutes(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new LimiteDeFrecuencia(1, Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void laPurgaAutomaticaOlvidaLasClavesCaducadasAlPasarDeMil() {
    for (int i = 0; i < 1001; i++) {
      limite.registrar("k" + i, T0);
    }
    assertThat(limite.clavesRecordadas()).isEqualTo(1001);

    limite.registrar("nueva", T0.plusSeconds(61));

    assertThat(limite.clavesRecordadas()).isEqualTo(1);
  }

  @Test
  void conMilClavesOMenosNoSePurgaAutomaticamente() {
    for (int i = 0; i < 1000; i++) {
      limite.registrar("k" + i, T0);
    }

    limite.registrar("nueva", T0.plusSeconds(61));

    assertThat(limite.clavesRecordadas()).isEqualTo(1001);
  }
}
