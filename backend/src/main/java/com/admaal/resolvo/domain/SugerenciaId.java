package com.admaal.resolvo.domain;

import java.util.UUID;

public record SugerenciaId(UUID valor) {
  public static SugerenciaId nuevo() {
    return new SugerenciaId(UUID.randomUUID());
  }
}
