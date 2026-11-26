package com.admaal.resolvo.infrastructure;

import com.admaal.resolvo.domain.Limite;
import java.time.Duration;

/** Crea un limite por ambito (login, ia, comentarios). Los ambitos no comparten cupo. */
@FunctionalInterface
public interface FabricaDeLimites {
  Limite crear(String ambito, int maximo, Duration ventana);
}
