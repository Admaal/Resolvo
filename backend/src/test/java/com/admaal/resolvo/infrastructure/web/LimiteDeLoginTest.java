package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.domain.LimiteDeFrecuencia;
import com.admaal.resolvo.infrastructure.FabricaDeLimites;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.RelojManual;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class LimiteDeLoginTest {

  private final RelojManual reloj = new RelojManual(Instant.parse("2026-11-19T10:00:00Z"));
  private static final String SECRETO = "secreto-de-prueba";
  private static final FabricaDeLimites EN_MEMORIA =
      (ambito, maximo, ventana) -> new LimiteDeFrecuencia(maximo, ventana);
  private final LimiteDeLogin limite = new LimiteDeLogin(reloj, EN_MEMORIA, 5, 15, SECRETO);

  private static MockHttpServletRequest desde(String cabecera, String remota) {
    MockHttpServletRequest peticion = new MockHttpServletRequest();
    peticion.setRemoteAddr(remota);
    if (cabecera != null) {
      peticion.addHeader("CF-Connecting-IP", cabecera);
    }
    return peticion;
  }

  @Test
  void laClaveUsaLaCabeceraDeCloudflareSiViene() {
    assertThat(limite.clave("Ana@Acme.test ", desde(" 203.0.113.9 ", "10.0.0.1")))
        .isEqualTo("ana@acme.test|203.0.113.9");
  }

  @Test
  void sinCabeceraOConCabeceraEnBlancoUsaLaDireccionRemota() {
    assertThat(limite.clave("a@b.test", desde(null, "10.0.0.1"))).isEqualTo("a@b.test|10.0.0.1");
    assertThat(limite.clave("a@b.test", desde("  ", "10.0.0.1"))).isEqualTo("a@b.test|10.0.0.1");
  }

  @Test
  void unEmailNuloNoRompeLaClave() {
    assertThat(limite.clave(null, desde(null, "10.0.0.1"))).isEqualTo("|10.0.0.1");
  }

  @Test
  void unEmailLargoSeRecortaParaNoAgrandarLaMemoria() {
    String clave = limite.clave("x".repeat(5000) + "@b.test", desde(null, "10.0.0.1"));

    assertThat(clave).hasSizeLessThanOrEqualTo(LimiteDeLogin.MAXIMO_DEL_EMAIL + "|10.0.0.1".length());
  }

  @Test
  void elQuintoIntentoPasaYElSextoSeRechazaConSuReintento() {
    for (int i = 0; i < 5; i++) {
      limite.intentar("a|1");
    }

    assertThatThrownBy(() -> limite.intentar("a|1"))
        .isInstanceOfSatisfying(
            DemasiadasPeticiones.class,
            error -> {
              assertThat(error.getMessage()).isEqualTo("demasiados-intentos");
              assertThat(error.segundosParaReintentar()).isEqualTo(900);
            });
  }

  @Test
  void unLoginCorrectoReiniciaLaCuenta() {
    for (int i = 0; i < 4; i++) {
      limite.intentar("a|1");
    }
    limite.correcto("a|1");

    for (int i = 0; i < 5; i++) {
      limite.intentar("a|1");
    }
    assertThatThrownBy(() -> limite.intentar("a|1")).isInstanceOf(DemasiadasPeticiones.class);
  }

  @Test
  void despuesDeLaVentanaVuelveAEntrar() {
    for (int i = 0; i < 5; i++) {
      limite.intentar("a|1");
    }
    reloj.avanzar(Duration.ofMinutes(15));

    limite.intentar("a|1");
  }

  @Test
  void intentosEnParaleloNoPasanDelMaximoAunqueLaContrasenaTarde() throws Exception {
    int hilos = 40;
    ExecutorService pool = Executors.newFixedThreadPool(hilos);
    CountDownLatch salida = new CountDownLatch(1);
    AtomicInteger pasan = new AtomicInteger();
    List<Future<?>> trabajos = new ArrayList<>();
    for (int i = 0; i < hilos; i++) {
      trabajos.add(
          pool.submit(
              () -> {
                salida.await();
                try {
                  limite.intentar("a|1");
                  pasan.incrementAndGet();
                } catch (DemasiadasPeticiones rechazado) {
                  // Esperado para todos menos cinco.
                }
                return null;
              }));
    }
    salida.countDown();
    for (Future<?> trabajo : trabajos) {
      trabajo.get();
    }
    pool.shutdown();

    assertThat(pasan.get()).isEqualTo(5);
  }

  private static MockHttpServletRequest firmada(String secreto, String ip) {
    MockHttpServletRequest peticion = desde("2a06:98c0:3600::103", "10.0.0.1");
    peticion.addHeader("X-Resolvo-Proxy", secreto);
    peticion.addHeader("X-Cliente-Ip", ip);
    return peticion;
  }

  @Test
  void conElSecretoCorrectoLaClaveUsaLaIpFirmadaDelProxy() {
    assertThat(limite.clave("a@b.test", firmada(SECRETO, "203.0.113.9")))
        .isEqualTo("a@b.test|203.0.113.9");
  }

  @Test
  void conUnSecretoIncorrectoSeIgnoraLaIpDeLaCabecera() {
    assertThat(limite.clave("a@b.test", firmada("adivinado", "203.0.113.9")))
        .isEqualTo("a@b.test|2a06:98c0:3600::103");
  }

  @Test
  void sinSecretoConfiguradoNuncaSeConfiaEnLaCabeceraAunqueVengaVacia() {
    LimiteDeLogin sinSecreto = new LimiteDeLogin(reloj, EN_MEMORIA, 5, 15, "");

    assertThat(sinSecreto.clave("a@b.test", firmada("", "203.0.113.9")))
        .isEqualTo("a@b.test|2a06:98c0:3600::103");
    assertThat(sinSecreto.clave("a@b.test", firmada("cualquiera", "203.0.113.9")))
        .isEqualTo("a@b.test|2a06:98c0:3600::103");
  }

  @Test
  void conSecretoCorrectoPeroSinIpCaeEnLaCabeceraDeCloudflare() {
    MockHttpServletRequest peticion = desde("198.51.100.4", "10.0.0.1");
    peticion.addHeader("X-Resolvo-Proxy", SECRETO);

    assertThat(limite.clave("a@b.test", peticion)).isEqualTo("a@b.test|198.51.100.4");
  }

  @Test
  void laIpFirmadaSeRecortaParaNoAgrandarLaMemoria() {
    String clave = limite.clave("a@b.test", firmada(SECRETO, "9".repeat(500)));

    assertThat(clave).hasSize("a@b.test|".length() + LimiteDeLogin.MAXIMO_DE_LA_IP);
  }

  @Test
  void clientesDistintosDetrasDelMismoProxyTienenClavesDistintas() {
    assertThat(limite.clave("a@b.test", firmada(SECRETO, "203.0.113.9")))
        .isNotEqualTo(limite.clave("a@b.test", firmada(SECRETO, "203.0.113.10")));
  }
}
