package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.PublicadorEventos;
import com.admaal.resolvo.domain.EventoTicket;
import java.util.ArrayList;
import java.util.List;

public final class PublicadorEnMemoria implements PublicadorEventos {

  private final List<EventoTicket> publicados = new ArrayList<>();

  @Override
  public void publicar(EventoTicket evento) {
    publicados.add(evento);
  }

  public List<EventoTicket> publicados() {
    return List.copyOf(publicados);
  }

  public void vaciar() {
    publicados.clear();
  }
}
