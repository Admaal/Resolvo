package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.SesionesRefresh;
import com.admaal.resolvo.domain.BonoRefresh;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;

public final class SesionesEnMemoria implements SesionesRefresh {

  private final LinkedHashMap<String, BonoRefresh> porToken = new LinkedHashMap<>();
  private int secuencia;

  @Override
  public void guardar(BonoRefresh bono) {
    porToken.put(bono.token(), bono);
  }

  @Override
  public Optional<BonoRefresh> porToken(String token) {
    return Optional.ofNullable(porToken.get(token)).map(SesionesEnMemoria::copia);
  }

  @Override
  public void revocarFamilia(UUID familia) {
    porToken.values().stream()
        .filter(bono -> bono.familia().equals(familia))
        .forEach(BonoRefresh::revocar);
  }

  @Override
  public String tokenNuevo() {
    secuencia++;
    return "refresh-" + secuencia;
  }

  private static BonoRefresh copia(BonoRefresh bono) {
    return new BonoRefresh(
        bono.token(), bono.usuarioId(), bono.familia(), bono.consumido(), bono.revocado());
  }
}
