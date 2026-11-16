package com.admaal.resolvo.domain;

import java.util.UUID;

public record FragmentoId(UUID valor) {
  public static FragmentoId nuevo() {
    return new FragmentoId(UUID.randomUUID());
  }
}
