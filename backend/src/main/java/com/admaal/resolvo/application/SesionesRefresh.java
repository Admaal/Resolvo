package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.BonoRefresh;
import java.util.Optional;
import java.util.UUID;

public interface SesionesRefresh {
  void guardar(BonoRefresh bono);

  Optional<BonoRefresh> porToken(String token);

  void revocarFamilia(UUID familia);

  String tokenNuevo();
}
