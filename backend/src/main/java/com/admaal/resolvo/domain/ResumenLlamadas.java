package com.admaal.resolvo.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Lo que costaron y tardaron las llamadas a la IA, por operacion y modelo. El coste es a precio de
 * lista y es nulo cuando un modelo no tiene tarifa: no se inventa un cero.
 */
public record ResumenLlamadas(List<Fila> filas) {

  public record Fila(
      String operacion,
      String modelo,
      int llamadas,
      int fallos,
      long tokensEntrada,
      long tokensSalida,
      long p50Ms,
      long p95Ms,
      BigDecimal costeUsd) {

    /** Tokens por llamada que funciono; las fallidas no gastan tokens y no cuentan. */
    public double tokensMedios() {
      int buenas = llamadas - fallos;
      return buenas == 0 ? 0.0 : (double) (tokensEntrada + tokensSalida) / buenas;
    }
  }

  public ResumenLlamadas {
    filas = List.copyOf(filas);
  }

  public static ResumenLlamadas de(
      List<LlamadaIa> llamadas, BiFunction<String, LocalDate, Optional<Tarifa>> tarifas) {
    Map<List<String>, List<LlamadaIa>> grupos = new LinkedHashMap<>();
    llamadas.stream()
        .sorted(Comparator.comparing(LlamadaIa::operacion).thenComparing(LlamadaIa::modelo))
        .forEach(
            llamada ->
                grupos
                    .computeIfAbsent(
                        List.of(llamada.operacion(), llamada.modelo()), clave -> new ArrayList<>())
                    .add(llamada));
    List<Fila> filas = new ArrayList<>();
    grupos.forEach(
        (clave, grupo) -> filas.add(fila(clave.get(0), clave.get(1), grupo, tarifas)));
    return new ResumenLlamadas(filas);
  }

  private static Fila fila(
      String operacion,
      String modelo,
      List<LlamadaIa> grupo,
      BiFunction<String, LocalDate, Optional<Tarifa>> tarifas) {
    long entrada = grupo.stream().mapToLong(LlamadaIa::tokensEntrada).sum();
    long salida = grupo.stream().mapToLong(LlamadaIa::tokensSalida).sum();
    List<Long> duraciones = new ArrayList<>(grupo.stream().map(LlamadaIa::milisegundos).toList());
    Collections.sort(duraciones);
    return new Fila(
        operacion,
        modelo,
        grupo.size(),
        (int) grupo.stream().filter(llamada -> !llamada.exito()).count(),
        entrada,
        salida,
        percentil(duraciones, 0.50),
        percentil(duraciones, 0.95),
        coste(grupo, tarifas));
  }

  /**
   * Cada llamada paga la tarifa vigente el dia (UTC) en que ocurrio: un precio que cambia a mitad
   * del periodo no se aplica hacia atras. Nulo si alguna llamada no tiene tarifa.
   */
  private static BigDecimal coste(
      List<LlamadaIa> grupo, BiFunction<String, LocalDate, Optional<Tarifa>> tarifas) {
    BigDecimal total = BigDecimal.ZERO;
    for (LlamadaIa llamada : grupo) {
      LocalDate dia = LocalDate.ofInstant(llamada.ocurridaEn(), ZoneOffset.UTC);
      Optional<Tarifa> tarifa = tarifas.apply(llamada.modelo(), dia);
      if (tarifa.isEmpty()) {
        return null;
      }
      total = total.add(tarifa.get().coste(llamada.tokensEntrada(), llamada.tokensSalida()));
    }
    return total;
  }

  /** Rango mas cercano: el menor valor que deja al menos esa fraccion de las muestras por debajo. */
  private static long percentil(List<Long> ordenadas, double fraccion) {
    int rango = (int) Math.ceil(fraccion * ordenadas.size());
    return ordenadas.get(Math.max(rango, 1) - 1);
  }

  public int llamadas() {
    return filas.stream().mapToInt(Fila::llamadas).sum();
  }

  public int fallos() {
    return filas.stream().mapToInt(Fila::fallos).sum();
  }

  public long tokensEntrada() {
    return filas.stream().mapToLong(Fila::tokensEntrada).sum();
  }

  public long tokensSalida() {
    return filas.stream().mapToLong(Fila::tokensSalida).sum();
  }

  /** Suma de las filas; nulo si alguna no tiene tarifa, porque el total seria una cifra falsa. */
  public BigDecimal costeUsd() {
    BigDecimal total = BigDecimal.ZERO;
    for (Fila fila : filas) {
      if (fila.costeUsd() == null) {
        return null;
      }
      total = total.add(fila.costeUsd());
    }
    return total;
  }
}
