package com.admaal.resolvo.infrastructure.ai;

import com.admaal.resolvo.application.IaNoDisponible;
import java.util.List;
import java.util.concurrent.TimeoutException;

final class FalloDelProveedor {

  /** Fallos que dependen del modelo o del momento: otro modelo puede responder. */
  private static final List<String> SE_PUEDE_PROBAR_OTRO =
      List.of(
          "404",
          "429",
          "500",
          "502",
          "503",
          "504",
          "UNAVAILABLE",
          "RESOURCE_EXHAUSTED",
          "no longer available",
          "overloaded");

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

  /**
   * Un 403 (clave o permisos) o un error de programacion no se arreglan cambiando de modelo; un
   * modelo retirado, saturado, sin cuota o que no contesta a tiempo, si.
   */
  static boolean sePuedeProbarOtroModelo(Throwable error) {
    for (Throwable actual = error; actual != null; actual = actual.getCause()) {
      if (actual instanceof TimeoutException) {
        return true;
      }
      String mensaje = actual.getMessage();
      if (mensaje != null && SE_PUEDE_PROBAR_OTRO.stream().anyMatch(mensaje::contains)) {
        return true;
      }
      if (actual.getCause() == actual) {
        break;
      }
    }
    return false;
  }

  /** El mensaje mas interno, recortado: lo que ayuda a diagnosticar sin volcar la traza entera. */
  static String resumen(Throwable error) {
    Throwable interno = error;
    while (interno.getCause() != null && interno.getCause() != interno) {
      interno = interno.getCause();
    }
    String mensaje =
        interno.getMessage() == null ? interno.getClass().getSimpleName() : interno.getMessage();
    return mensaje.length() > 200 ? mensaje.substring(0, 200) : mensaje;
  }
}
