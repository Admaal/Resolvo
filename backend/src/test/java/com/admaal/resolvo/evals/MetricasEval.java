package com.admaal.resolvo.evals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Aritmetica de los evals: pura y sin red, para poder probarla en el gate normal. */
public final class MetricasEval {

  private MetricasEval() {}

  public static double proporcion(long aciertos, long total) {
    return total == 0 ? 0.0 : (double) aciertos / total;
  }

  /** Rango mas cercano: el menor valor que deja al menos el 95 % de las muestras por debajo. */
  public static long p95(List<Long> muestras) {
    if (muestras.isEmpty()) {
      return 0;
    }
    List<Long> ordenadas = new ArrayList<>(muestras);
    Collections.sort(ordenadas);
    int rango = (int) Math.ceil(0.95 * ordenadas.size());
    return ordenadas.get(rango - 1);
  }

  public static double media(List<Integer> valores) {
    return valores.isEmpty()
        ? 0.0
        : valores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
  }

  /**
   * Un umbral de distancia que separa las consultas con respuesta de las que no la tienen: el punto
   * medio entre la peor distancia de las primeras y la mejor de las segundas. Si se solapan, ningun
   * umbral las separa y no hay recomendacion.
   */
  public static Optional<Double> umbralRecomendado(
      List<Double> distanciasConRespuesta, List<Double> distanciasSinRespuesta) {
    if (distanciasConRespuesta.isEmpty() || distanciasSinRespuesta.isEmpty()) {
      return Optional.empty();
    }
    double peorConRespuesta = Collections.max(distanciasConRespuesta);
    double mejorSinRespuesta = Collections.min(distanciasSinRespuesta);
    if (peorConRespuesta >= mejorSinRespuesta) {
      return Optional.empty();
    }
    return Optional.of((peorConRespuesta + mejorSinRespuesta) / 2);
  }
}
