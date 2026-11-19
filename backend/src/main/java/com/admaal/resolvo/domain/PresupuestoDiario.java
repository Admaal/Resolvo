package com.admaal.resolvo.domain;

/**
 * Tope diario de uso de la IA de una organizacion. Las llamadas son un tope estricto (cuentan las
 * que estan en curso); los tokens, uno blando: solo se conocen al terminar cada llamada.
 */
public record PresupuestoDiario(int llamadas, long tokens) {

  public PresupuestoDiario {
    if (llamadas < 1 || tokens < 1) {
      throw new IllegalArgumentException("los limites deben ser positivos");
    }
  }

  public boolean admite(Consumo consumo, int enCurso) {
    return consumo.llamadas() + enCurso < llamadas && consumo.tokens() < tokens;
  }
}
