package com.admaal.resolvo.application;

/** El proveedor de IA no esta configurado, no responde o no tiene cuota. El motivo es un codigo estable. */
public final class IaNoDisponible extends RuntimeException {

  public IaNoDisponible(String motivo) {
    super(motivo);
  }

  public IaNoDisponible(String motivo, Throwable causa) {
    super(motivo, causa);
  }
}
