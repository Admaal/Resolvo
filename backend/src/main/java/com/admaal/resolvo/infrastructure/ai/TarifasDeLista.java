package com.admaal.resolvo.infrastructure.ai;

import com.admaal.resolvo.application.Tarifas;
import com.admaal.resolvo.domain.Tarifa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Precios de lista de los modelos, leidos de {@code resolvo.ia.tarifas} en application.yml. Cada
 * tarifa cita su fuente y la fecha en que se miro: no se inventa ningun precio, y un modelo sin
 * tarifa (por ejemplo un alias "latest") queda con el coste desconocido.
 */
@Component
public class TarifasDeLista implements Tarifas {

  /** Una entrada del yaml. */
  public record Entrada(
      String modelo,
      BigDecimal entradaUsdPorMillon,
      BigDecimal salidaUsdPorMillon,
      String fuente,
      LocalDate fecha) {}

  private final List<Entrada> entradas;
  private final Map<String, Tarifa> porModelo = new HashMap<>();

  @Autowired
  public TarifasDeLista(Environment entorno) {
    this(Binder.get(entorno));
  }

  TarifasDeLista(Binder binder) {
    this.entradas =
        binder.bind("resolvo.ia.tarifas", Bindable.listOf(Entrada.class)).orElse(List.of());
    for (Entrada entrada : entradas) {
      if (entrada.fuente() == null || entrada.fuente().isBlank() || entrada.fecha() == null) {
        throw new IllegalStateException(
            "La tarifa de " + entrada.modelo() + " necesita fuente y fecha");
      }
      Tarifa tarifa =
          new Tarifa(entrada.modelo(), entrada.entradaUsdPorMillon(), entrada.salidaUsdPorMillon());
      if (porModelo.put(entrada.modelo(), tarifa) != null) {
        throw new IllegalStateException("Tarifa repetida para " + entrada.modelo());
      }
    }
  }

  @Override
  public Optional<Tarifa> de(String modelo) {
    return Optional.ofNullable(porModelo.get(modelo));
  }

  public List<Entrada> entradas() {
    return entradas;
  }
}
