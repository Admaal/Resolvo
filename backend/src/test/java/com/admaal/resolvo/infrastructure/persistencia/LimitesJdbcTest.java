package com.admaal.resolvo.infrastructure.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.domain.Limite;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
class LimitesJdbcTest {

  private static final Instant T0 = Instant.parse("2026-11-26T10:00:00Z");
  private static final Duration MINUTO = Duration.ofMinutes(1);

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate transacciones;

  @BeforeEach
  void limpiar() {
    jdbc.update("delete from limite_marca where ambito like 'prueba-%'");
  }

  private Limite limite(String ambito, int maximo) {
    return new LimitesJdbc(jdbc, transacciones).crear(ambito, maximo, MINUTO);
  }

  @Test
  void elQuintoPasaYElSextoSeRechazaConSuReintento() {
    Limite limite = limite("prueba-a", 5);
    for (int i = 0; i < 5; i++) {
      assertThat(limite.admitir("clave", T0.plusSeconds(i)).admitida()).isTrue();
    }

    Limite.Decision sexto = limite.admitir("clave", T0.plusSeconds(20));

    assertThat(sexto.admitida()).isFalse();
    assertThat(sexto.reintentarEn()).isEqualTo(Duration.ofSeconds(40));
  }

  @Test
  void sobreviveAUnaNuevaInstancia() {
    Limite antes = limite("prueba-b", 2);
    antes.admitir("clave", T0);
    antes.admitir("clave", T0);

    Limite despuesDeReiniciar = limite("prueba-b", 2);

    assertThat(despuesDeReiniciar.admitir("clave", T0.plusSeconds(1)).admitida()).isFalse();
  }

  @Test
  void pasadaLaVentanaVuelveAEntrar() {
    Limite limite = limite("prueba-c", 1);
    limite.admitir("clave", T0);

    assertThat(limite.admitir("clave", T0.plusSeconds(59)).admitida()).isFalse();
    assertThat(limite.admitir("clave", T0.plusSeconds(60)).admitida()).isTrue();
  }

  @Test
  void reiniciarBorraLasMarcasDeEsaClave() {
    Limite limite = limite("prueba-d", 1);
    limite.admitir("clave", T0);
    limite.admitir("otra", T0);

    limite.reiniciar("clave");

    assertThat(limite.admitir("clave", T0.plusSeconds(1)).admitida()).isTrue();
    assertThat(limite.admitir("otra", T0.plusSeconds(1)).admitida()).isFalse();
  }

  @Test
  void dosAmbitosNoCompartenCupo() {
    limite("prueba-e", 1).admitir("clave", T0);

    assertThat(limite("prueba-f", 1).admitir("clave", T0).admitida()).isTrue();
  }

  @Test
  void noGuardaLaClaveEnClaro() {
    limite("prueba-g", 3).admitir("ana@acme.test|203.0.113.7", T0);

    List<String> claves =
        jdbc.queryForList("select clave from limite_marca where ambito = 'prueba-g'", String.class);

    assertThat(claves).hasSize(1);
    assertThat(claves.get(0)).doesNotContain("ana").doesNotContain("203.0.113.7").hasSize(64);
  }

  @Test
  void dosHilosNoPasanJuntosDelMaximo() throws Exception {
    Limite limite = limite("prueba-h", 5);
    ExecutorService hilos = Executors.newFixedThreadPool(8);
    CountDownLatch salida = new CountDownLatch(1);
    List<Callable<Boolean>> tareas = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      tareas.add(
          () -> {
            salida.await();
            return limite.admitir("clave", T0).admitida();
          });
    }
    List<Future<Boolean>> resultados = new ArrayList<>();
    for (Callable<Boolean> tarea : tareas) {
      resultados.add(hilos.submit(tarea));
    }
    salida.countDown();
    int admitidas = 0;
    for (Future<Boolean> resultado : resultados) {
      admitidas += resultado.get() ? 1 : 0;
    }
    hilos.shutdown();

    assertThat(admitidas).isEqualTo(5);
  }

  @Test
  void purgarQuitaLasMarcasDeMasDeUnDia() {
    Limite limite = limite("prueba-i", 5);
    limite.admitir("vieja", T0);
    limite.admitir("nueva", T0.plus(Duration.ofHours(23)));

    new LimitesJdbc(jdbc, transacciones).purgar(T0.plus(Duration.ofHours(25)));

    assertThat(
            jdbc.queryForObject(
                "select count(*) from limite_marca where ambito = 'prueba-i'", Integer.class))
        .isEqualTo(1);
  }
}
