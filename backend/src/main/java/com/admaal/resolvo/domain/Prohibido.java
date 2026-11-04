package com.admaal.resolvo.domain;

public final class Prohibido extends RuntimeException {
  public Prohibido() {
    super("prohibido");
  }
}
