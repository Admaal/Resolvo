package com.admaal.resolvo.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

class TarifasDeListaTest {

  private static final LocalDate HOY = LocalDate.parse("2026-11-26");

  private static Binder binderDe(String yaml) throws IOException {
    MutablePropertySources fuentes = new MutablePropertySources();
    List<PropertySource<?>> cargadas =
        new YamlPropertySourceLoader().load("t", new ByteArrayResource(yaml.getBytes()));
    cargadas.forEach(fuentes::addLast);
    return new Binder(ConfigurationPropertySources.from(fuentes));
  }

  @Test
  void lasTarifasDelApplicationYmlCitanFuenteYFechaYTienenPreciosValidos() throws IOException {
    MutablePropertySources fuentes = new MutablePropertySources();
    new YamlPropertySourceLoader()
        .load("application", new ClassPathResource("application.yml"))
        .forEach(fuentes::addLast);
    fuentes.addLast(new MapPropertySource("vacio", java.util.Map.of()));

    TarifasDeLista tarifas = new TarifasDeLista(new Binder(ConfigurationPropertySources.from(fuentes)));

    assertThat(tarifas.entradas()).isNotEmpty();
    assertThat(tarifas.entradas())
        .allSatisfy(
            entrada -> {
              assertThat(entrada.fuente()).startsWith("https://");
              assertThat(entrada.fecha()).isNotNull();
              assertThat(entrada.entradaUsdPorMillon()).isPositive();
              assertThat(entrada.salidaUsdPorMillon()).isPositive();
              LocalDate vigente = entrada.desde() == null ? HOY : entrada.desde();
              assertThat(tarifas.de(entrada.modelo(), vigente)).isPresent();
            });
    assertThat(tarifas.entradas())
        .extracting(entrada -> entrada.modelo() + "@" + entrada.desde())
        .doesNotHaveDuplicates();
  }

  @Test
  void unAliasSinPrecioPublicadoNoTieneTarifa() throws IOException {
    TarifasDeLista tarifas = new TarifasDeLista(binderDe(YAML));

    assertThat(tarifas.de("gemini-flash-lite-latest", HOY)).isEmpty();
    assertThat(tarifas.de("m1", HOY)).isPresent();
    assertThat(tarifas.de("m1", HOY).orElseThrow().coste(1_000_000, 1_000_000)).isEqualByComparingTo("3.00");
  }

  @Test
  void sinTarifasConfiguradasNoHayNinguna() throws IOException {
    assertThat(new TarifasDeLista(binderDe("otra:\n  cosa: 1\n")).entradas()).isEmpty();
  }

  @Test
  void unaTarifaSinFuenteOSinFechaNoArranca() {
    assertThatThrownBy(
            () ->
                new TarifasDeLista(
                    binderDe(
                        "resolvo:\n  ia:\n    tarifas:\n      - modelo: m1\n        entrada-usd-por-millon: 1\n        salida-usd-por-millon: 2\n        fecha: 2026-11-19\n")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("m1");
    assertThatThrownBy(
            () ->
                new TarifasDeLista(
                    binderDe(
                        "resolvo:\n  ia:\n    tarifas:\n      - modelo: m1\n        entrada-usd-por-millon: 1\n        salida-usd-por-millon: 2\n        fuente: https://x\n")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void dosTarifasParaElMismoModeloNoArrancan() {
    String repetida =
        "resolvo:\n  ia:\n    tarifas:\n"
            + "      - {modelo: m1, entrada-usd-por-millon: 1, salida-usd-por-millon: 2, fuente: 'https://x', fecha: 2026-11-19}\n"
            + "      - {modelo: m1, entrada-usd-por-millon: 1, salida-usd-por-millon: 2, fuente: 'https://x', fecha: 2026-11-19}\n";

    assertThatThrownBy(() -> new TarifasDeLista(binderDe(repetida)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("repetida");
  }

  @Test
  void eligeLaTarifaVigenteElDiaDeLaLlamada() throws IOException {
    String yaml =
        "resolvo:\n  ia:\n    tarifas:\n"
            + "      - {modelo: m1, entrada-usd-por-millon: 1, salida-usd-por-millon: 2, fuente: 'https://x', fecha: 2026-11-19}\n"
            + "      - {modelo: m1, entrada-usd-por-millon: 3, salida-usd-por-millon: 4, fuente: 'https://x', fecha: 2026-11-19, desde: 2027-01-01}\n";
    TarifasDeLista tarifas = new TarifasDeLista(binderDe(yaml));

    assertThat(tarifas.de("m1", LocalDate.parse("2026-12-31")).orElseThrow().usdPorMillonEntrada())
        .isEqualByComparingTo("1");
    assertThat(tarifas.de("m1", LocalDate.parse("2027-01-01")).orElseThrow().usdPorMillonEntrada())
        .isEqualByComparingTo("3");
  }

  @Test
  void unaTarifaQueEmpiezaMasTardeNoValeAntesDeSuFecha() throws IOException {
    String yaml =
        "resolvo:\n  ia:\n    tarifas:\n"
            + "      - {modelo: m2, entrada-usd-por-millon: 1, salida-usd-por-millon: 2, fuente: 'https://x', fecha: 2026-11-19, desde: 2027-01-01}\n";
    TarifasDeLista tarifas = new TarifasDeLista(binderDe(yaml));

    assertThat(tarifas.de("m2", LocalDate.parse("2026-12-31"))).isEmpty();
    assertThat(tarifas.de("m2", LocalDate.parse("2027-01-01"))).isPresent();
  }

  @Test
  void lasTarifasDel2027DelApplicationYmlDuplicanElPrecioDeLosFlash() throws IOException {
    MutablePropertySources fuentes = new MutablePropertySources();
    new YamlPropertySourceLoader()
        .load("application", new ClassPathResource("application.yml"))
        .forEach(fuentes::addLast);
    TarifasDeLista tarifas = new TarifasDeLista(new Binder(ConfigurationPropertySources.from(fuentes)));

    for (String modelo : List.of("gemini-3.8-flash", "gemini-3.6-flash")) {
      var antes = tarifas.de(modelo, LocalDate.parse("2026-12-31")).orElseThrow();
      var despues = tarifas.de(modelo, LocalDate.parse("2027-01-01")).orElseThrow();
      assertThat(despues.usdPorMillonEntrada())
          .isEqualByComparingTo(antes.usdPorMillonEntrada().multiply(java.math.BigDecimal.TWO));
      assertThat(despues.usdPorMillonSalida())
          .isEqualByComparingTo(antes.usdPorMillonSalida().multiply(java.math.BigDecimal.TWO));
    }
  }

  private static final String YAML =
      "resolvo:\n  ia:\n    tarifas:\n"
          + "      - {modelo: m1, entrada-usd-por-millon: 1, salida-usd-por-millon: 2, fuente: 'https://x', fecha: 2026-11-19}\n";
}
