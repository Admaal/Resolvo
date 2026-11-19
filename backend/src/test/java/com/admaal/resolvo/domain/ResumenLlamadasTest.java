package com.admaal.resolvo.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResumenLlamadasTest {

  private static final Instant AHORA = Instant.parse("2026-11-19T10:00:00Z");
  private final OrganizacionId acme = new OrganizacionId(UUID.randomUUID());

  private LlamadaIa llamada(String operacion, String modelo, int entrada, int salida, long ms) {
    return new LlamadaIa(UUID.randomUUID(), acme, operacion, modelo, entrada, salida, ms, true, AHORA);
  }

  private LlamadaIa fallo(String operacion, String modelo, long ms) {
    return new LlamadaIa(UUID.randomUUID(), acme, operacion, modelo, 0, 0, ms, false, AHORA);
  }

  private static Optional<Tarifa> sinTarifas(String modelo) {
    return Optional.empty();
  }

  @Test
  void sinLlamadasNoHayFilasNiTotales() {
    ResumenLlamadas resumen = ResumenLlamadas.de(List.of(), ResumenLlamadasTest::sinTarifas);

    assertThat(resumen.filas()).isEmpty();
    assertThat(resumen.llamadas()).isZero();
    assertThat(resumen.fallos()).isZero();
    assertThat(resumen.tokensEntrada()).isZero();
    assertThat(resumen.tokensSalida()).isZero();
  }

  @Test
  void agrupaPorOperacionYModeloYSumaTokens() {
    List<LlamadaIa> llamadas =
        List.of(
            llamada("sugerir", "m1", 100, 20, 1_000),
            llamada("sugerir", "m1", 300, 40, 3_000),
            llamada("sugerir", "m2", 50, 5, 500),
            llamada("clasificar", "m1", 10, 2, 100));

    ResumenLlamadas resumen = ResumenLlamadas.de(llamadas, ResumenLlamadasTest::sinTarifas);

    assertThat(resumen.filas())
        .extracting(
            ResumenLlamadas.Fila::operacion, ResumenLlamadas.Fila::modelo, ResumenLlamadas.Fila::llamadas)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("clasificar", "m1", 1),
            org.assertj.core.groups.Tuple.tuple("sugerir", "m1", 2),
            org.assertj.core.groups.Tuple.tuple("sugerir", "m2", 1));
    ResumenLlamadas.Fila sugerirM1 = resumen.filas().get(1);
    assertThat(sugerirM1.tokensEntrada()).isEqualTo(400);
    assertThat(sugerirM1.tokensSalida()).isEqualTo(60);
    assertThat(resumen.llamadas()).isEqualTo(4);
    assertThat(resumen.tokensEntrada()).isEqualTo(460);
    assertThat(resumen.tokensSalida()).isEqualTo(67);
  }

  @Test
  void cuentaLosFallosYLosTokensMediosSoloDeLasLlamadasQueFuncionaron() {
    List<LlamadaIa> llamadas =
        List.of(
            llamada("sugerir", "m1", 100, 20, 1_000),
            llamada("sugerir", "m1", 200, 40, 2_000),
            fallo("sugerir", "m1", 15_000));

    ResumenLlamadas resumen = ResumenLlamadas.de(llamadas, ResumenLlamadasTest::sinTarifas);

    ResumenLlamadas.Fila fila = resumen.filas().get(0);
    assertThat(fila.llamadas()).isEqualTo(3);
    assertThat(fila.fallos()).isEqualTo(1);
    assertThat(fila.tokensMedios()).isEqualTo(180.0);
    assertThat(resumen.fallos()).isEqualTo(1);
  }

  @Test
  void unaFilaSoloConFallosTieneTokensMediosCero() {
    ResumenLlamadas resumen =
        ResumenLlamadas.de(List.of(fallo("sugerir", "m1", 15_000)), ResumenLlamadasTest::sinTarifas);

    assertThat(resumen.filas().get(0).tokensMedios()).isZero();
  }

  @Test
  void laLatenciaIncluyeLosFallosYUsaElRangoMasCercano() {
    List<LlamadaIa> llamadas = new ArrayList<>();
    for (int ms = 1; ms <= 19; ms++) {
      llamadas.add(llamada("sugerir", "m1", 1, 1, ms * 100L));
    }
    llamadas.add(fallo("sugerir", "m1", 20_000));

    ResumenLlamadas.Fila fila =
        ResumenLlamadas.de(llamadas, ResumenLlamadasTest::sinTarifas).filas().get(0);

    assertThat(fila.p50Ms()).isEqualTo(1_000);
    assertThat(fila.p95Ms()).isEqualTo(1_900);
  }

  @Test
  void laLatenciaNoDependeDelOrdenEnQueLlegaron() {
    List<LlamadaIa> llamadas = new ArrayList<>();
    for (int ms = 19; ms >= 1; ms--) {
      llamadas.add(llamada("sugerir", "m1", 1, 1, ms * 100L));
    }
    llamadas.add(fallo("sugerir", "m1", 20_000));

    ResumenLlamadas.Fila fila =
        ResumenLlamadas.de(llamadas, ResumenLlamadasTest::sinTarifas).filas().get(0);

    assertThat(fila.p50Ms()).isEqualTo(1_000);
    assertThat(fila.p95Ms()).isEqualTo(1_900);
  }

  @Test
  void conUnaSolaLlamadaTodosLosPercentilesSonSuDuracion() {
    ResumenLlamadas.Fila fila =
        ResumenLlamadas.de(
                List.of(llamada("sugerir", "m1", 1, 1, 777)), ResumenLlamadasTest::sinTarifas)
            .filas()
            .get(0);

    assertThat(fila.p50Ms()).isEqualTo(777);
    assertThat(fila.p95Ms()).isEqualTo(777);
  }

  @Test
  void elCosteSaleDeLaTarifaDelModeloALosPreciosDeLista() {
    Tarifa tarifa = new Tarifa("m1", new BigDecimal("0.10"), new BigDecimal("0.40"));
    List<LlamadaIa> llamadas =
        List.of(
            llamada("sugerir", "m1", 1_000, 500, 1_000), llamada("sugerir", "m1", 2_000, 0, 1_000));

    ResumenLlamadas resumen =
        ResumenLlamadas.de(llamadas, modelo -> "m1".equals(modelo) ? Optional.of(tarifa) : Optional.empty());

    // 3000 entrada a 0,10 USD por millon + 500 salida a 0,40 USD por millon
    assertThat(resumen.filas().get(0).costeUsd()).isEqualByComparingTo("0.0005");
    assertThat(resumen.costeUsd()).isEqualByComparingTo("0.0005");
  }

  @Test
  void sinTarifaElCosteEsDesconocidoYNoCero() {
    List<LlamadaIa> llamadas =
        List.of(llamada("sugerir", "m1", 1_000, 500, 1_000), llamada("sugerir", "m2", 1_000, 500, 1_000));
    Tarifa tarifa = new Tarifa("m1", new BigDecimal("0.10"), new BigDecimal("0.40"));

    ResumenLlamadas resumen =
        ResumenLlamadas.de(llamadas, modelo -> "m1".equals(modelo) ? Optional.of(tarifa) : Optional.empty());

    assertThat(resumen.filas().get(0).costeUsd()).isNotNull();
    assertThat(resumen.filas().get(1).costeUsd()).isNull();
    assertThat(resumen.costeUsd()).isNull();
  }
}
