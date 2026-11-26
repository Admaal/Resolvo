package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Tarifa;
import java.time.LocalDate;
import java.util.Optional;

@FunctionalInterface
public interface Tarifas {
  /** La tarifa del modelo vigente ese dia (UTC), o vacio si no se conoce. */
  Optional<Tarifa> de(String modelo, LocalDate dia);
}
