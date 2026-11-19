package com.admaal.resolvo.infrastructure.web;

import java.time.Duration;

/** Se superó un límite de uso: 429 con el código en el cuerpo y Retry-After. */
public final class DemasiadasPeticiones extends RuntimeException {

  private final Duration reintentarEn;

  public DemasiadasPeticiones(String codigo, Duration reintentarEn) {
    super(codigo);
    this.reintentarEn = reintentarEn;
  }

  /** Segundos para la cabecera Retry-After: redondeo hacia arriba y nunca menos de 1. */
  public long segundosParaReintentar() {
    return Math.max(1, (reintentarEn.toMillis() + 999) / 1000);
  }
}
