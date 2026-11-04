package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.BonoRefresh;

public final class CerrarSesion {

  private final SesionesRefresh sesiones;

  public CerrarSesion(SesionesRefresh sesiones) {
    this.sesiones = sesiones;
  }

  public void ejecutar(String token) {
    sesiones.porToken(token).ifPresent(bono -> invalidar(bono));
  }

  private void invalidar(BonoRefresh bono) {
    sesiones.revocarFamilia(bono.familia());
  }
}
