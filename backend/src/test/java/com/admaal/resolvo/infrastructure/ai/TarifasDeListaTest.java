package com.admaal.resolvo.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
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
              assertThat(tarifas.de(entrada.modelo())).isPresent();
            });
    assertThat(tarifas.entradas()).extracting(TarifasDeLista.Entrada::modelo).doesNotHaveDuplicates();
  }

  @Test
  void unAliasSinPrecioPublicadoNoTieneTarifa() throws IOException {
    TarifasDeLista tarifas = new TarifasDeLista(binderDe(YAML));

    assertThat(tarifas.de("gemini-flash-lite-latest")).isEmpty();
    assertThat(tarifas.de("m1")).isPresent();
    assertThat(tarifas.de("m1").orElseThrow().coste(1_000_000, 1_000_000)).isEqualByComparingTo("3.00");
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

  private static final String YAML =
      "resolvo:\n  ia:\n    tarifas:\n"
          + "      - {modelo: m1, entrada-usd-por-millon: 1, salida-usd-por-millon: 2, fuente: 'https://x', fecha: 2026-11-19}\n";
}
