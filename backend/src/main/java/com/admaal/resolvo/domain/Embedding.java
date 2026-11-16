package com.admaal.resolvo.domain;

public record Embedding(float[] valores) {

  public static final int DIMENSION = 768;

  public Embedding {
    if (valores == null || valores.length != DIMENSION) {
      throw new IllegalArgumentException("dimension invalida");
    }
    for (float valor : valores) {
      if (!Float.isFinite(valor)) {
        throw new IllegalArgumentException("componente no finita");
      }
    }
    valores = valores.clone();
  }

  @Override
  public float[] valores() {
    return valores.clone();
  }
}
