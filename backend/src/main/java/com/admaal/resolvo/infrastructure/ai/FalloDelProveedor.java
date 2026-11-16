package com.admaal.resolvo.infrastructure.ai;

import com.admaal.resolvo.application.IaNoDisponible;

final class FalloDelProveedor {

  private FalloDelProveedor() {}

  /** Traduce cualquier error del SDK a un codigo estable, sin filtrar mensajes del proveedor. */
  static IaNoDisponible de(Throwable error) {
    for (Throwable actual = error; actual != null; actual = actual.getCause()) {
      String mensaje = actual.getMessage();
      if (mensaje != null && (mensaje.contains("429") || mensaje.contains("RESOURCE_EXHAUSTED"))) {
        return new IaNoDisponible("ia-cuota-agotada", error);
      }
      if (actual.getCause() == actual) {
        break;
      }
    }
    return new IaNoDisponible("ia-no-disponible", error);
  }
}
