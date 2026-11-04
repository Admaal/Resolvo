package com.admaal.resolvo.domain;

public final class CredencialesInvalidas extends RuntimeException {
  public CredencialesInvalidas() {
    super("no-autorizado");
  }
}
