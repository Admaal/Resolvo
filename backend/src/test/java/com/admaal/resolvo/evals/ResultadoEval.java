package com.admaal.resolvo.evals;

/** Una metrica medida contra su umbral. La unidad "%" significa que el valor es una proporcion 0..1. */
public record ResultadoEval(String nombre, double valor, double umbral, Sentido sentido, String unidad) {

  public enum Sentido {
    /** El valor debe llegar al umbral: calidad. */
    MINIMO,
    /** El valor no debe superar el umbral: coste y latencia. */
    MAXIMO
  }

  public boolean cumple() {
    return sentido == Sentido.MINIMO ? valor >= umbral : valor <= umbral;
  }
}
