package com.admaal.resolvo.domain;

import java.time.Instant;
import java.util.UUID;

public record ComentarioId(UUID valor) {
  public static ComentarioId nuevo() {
    return new ComentarioId(UUID.randomUUID());
  }
}
