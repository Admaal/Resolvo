package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.ResumenLlamadas;
import com.admaal.resolvo.domain.Rol;
import java.time.Instant;

/** Tokens, coste y latencia de la IA de una organizacion. Solo la ve un ADMIN. */
public final class ConsultarMetricasIa {

  private final RegistroLlamadas registro;
  private final Tarifas tarifas;

  public ConsultarMetricasIa(RegistroLlamadas registro, Tarifas tarifas) {
    this.registro = registro;
    this.tarifas = tarifas;
  }

  public ResumenLlamadas ejecutar(
      OrganizacionId organizacionId, Rol rol, Instant desde, Instant hasta) {
    if (rol != Rol.ADMIN) {
      throw new Prohibido();
    }
    if (desde.isAfter(hasta)) {
      throw new IllegalArgumentException("el rango termina antes de empezar");
    }
    return ResumenLlamadas.de(registro.deLaOrganizacion(organizacionId, desde, hasta), tarifas::de);
  }
}
