package com.admaal.resolvo.domain;

import java.time.Duration;
import java.time.Instant;

/** Limite de peticiones por clave en una ventana deslizante. El tiempo lo pone quien llama. */
public interface Limite {

  record Decision(boolean admitida, Duration reintentarEn) {}

  /** Mira y cuenta en un solo paso: un rechazo no consume cupo. */
  Decision admitir(String clave, Instant ahora);

  /** Olvida las peticiones contadas de la clave. */
  void reiniciar(String clave);
}
