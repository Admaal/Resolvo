package com.admaal.resolvo.domain;

import java.util.UUID;

public record ClasificacionId(UUID valor) {
  public static ClasificacionId nuevo() {
    return new ClasificacionId(UUID.randomUUID());
  }
}
