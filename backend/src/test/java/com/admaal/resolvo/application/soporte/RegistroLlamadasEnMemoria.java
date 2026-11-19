package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.RegistroLlamadas;
import com.admaal.resolvo.domain.Consumo;
import com.admaal.resolvo.domain.LlamadaIa;
import com.admaal.resolvo.domain.OrganizacionId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class RegistroLlamadasEnMemoria implements RegistroLlamadas {

  private final List<LlamadaIa> llamadas = new ArrayList<>();

  @Override
  public void registrar(LlamadaIa llamada) {
    llamadas.add(llamada);
  }

  public List<LlamadaIa> todas() {
    return List.copyOf(llamadas);
  }

  @Override
  public List<LlamadaIa> deLaOrganizacion(OrganizacionId organizacionId, Instant desde, Instant hasta) {
    return llamadas.stream()
        .filter(l -> organizacionId.equals(l.organizacionId()))
        .filter(l -> !l.ocurridaEn().isBefore(desde) && l.ocurridaEn().isBefore(hasta))
        .toList();
  }

  @Override
  public Consumo consumoDe(OrganizacionId organizacionId, Instant desde, Instant hasta) {
    List<LlamadaIa> deLaOrganizacion = deLaOrganizacion(organizacionId, desde, hasta);
    long tokens = 0;
    for (LlamadaIa llamada : deLaOrganizacion) {
      tokens += llamada.tokensEntrada() + llamada.tokensSalida();
    }
    return new Consumo(deLaOrganizacion.size(), tokens);
  }
}
