package com.admaal.resolvo.domain;

public final class TransicionNoPermitida extends RuntimeException {
  public TransicionNoPermitida() {
    super("conflicto");
  }
}
