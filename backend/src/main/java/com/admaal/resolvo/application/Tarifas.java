package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Tarifa;
import java.util.Optional;

@FunctionalInterface
public interface Tarifas {
  Optional<Tarifa> de(String modelo);
}
