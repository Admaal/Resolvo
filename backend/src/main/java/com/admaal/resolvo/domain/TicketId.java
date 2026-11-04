package com.admaal.resolvo.domain;

import java.util.UUID;

public record TicketId(UUID valor) {
  public static TicketId nuevo() {
    return new TicketId(UUID.randomUUID());
  }
}
