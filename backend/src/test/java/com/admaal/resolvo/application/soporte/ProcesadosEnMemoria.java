package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.EventosProcesados;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ProcesadosEnMemoria implements EventosProcesados {

  private final Set<String> hechos = new HashSet<>();

  @Override
  public boolean yaProcesado(String consumidor, UUID eventoId) {
    return hechos.contains(consumidor + "/" + eventoId);
  }

  @Override
  public void registrar(String consumidor, UUID eventoId) {
    hechos.add(consumidor + "/" + eventoId);
  }
}
