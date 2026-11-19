package com.admaal.resolvo.domain;

import java.math.BigDecimal;

/** Precio de lista de un modelo, en dolares por millon de tokens. */
public record Tarifa(String modelo, BigDecimal usdPorMillonEntrada, BigDecimal usdPorMillonSalida) {

  private static final BigDecimal MILLON = BigDecimal.valueOf(1_000_000);

  public Tarifa {
    if (modelo == null || modelo.isBlank()) {
      throw new IllegalArgumentException("la tarifa necesita un modelo");
    }
    if (usdPorMillonEntrada.signum() < 0 || usdPorMillonSalida.signum() < 0) {
      throw new IllegalArgumentException("un precio no puede ser negativo");
    }
  }

  public BigDecimal coste(long tokensEntrada, long tokensSalida) {
    return usdPorMillonEntrada
        .multiply(BigDecimal.valueOf(tokensEntrada))
        .add(usdPorMillonSalida.multiply(BigDecimal.valueOf(tokensSalida)))
        .divide(MILLON);
  }
}
