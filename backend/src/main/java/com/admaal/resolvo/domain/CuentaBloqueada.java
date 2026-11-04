package com.admaal.resolvo.domain;

public final class CuentaBloqueada extends RuntimeException {
  public CuentaBloqueada() {
    super("bloqueado");
  }
}
