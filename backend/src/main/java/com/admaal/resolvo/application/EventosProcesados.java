package com.admaal.resolvo.application;

import java.util.UUID;

/** Clave de idempotencia: que consumidor ya atendio que evento. */
public interface EventosProcesados {
  boolean yaProcesado(String consumidor, UUID eventoId);

  void registrar(String consumidor, UUID eventoId);
}
