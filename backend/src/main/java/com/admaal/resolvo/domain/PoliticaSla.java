package com.admaal.resolvo.domain;

import java.time.Duration;

public final class PoliticaSla {

  public Duration primeraRespuesta(String categoria, Prioridad prioridad) {
    boolean incidencia = "Incidencia".equals(categoria);
    return switch (prioridad) {
      case URGENTE -> Duration.ofHours(incidencia ? 1 : 2);
      case ALTA -> Duration.ofHours(incidencia ? 4 : 8);
      case MEDIA -> Duration.ofHours(incidencia ? 8 : 24);
      case BAJA -> Duration.ofHours(incidencia ? 24 : 48);
    };
  }
}
