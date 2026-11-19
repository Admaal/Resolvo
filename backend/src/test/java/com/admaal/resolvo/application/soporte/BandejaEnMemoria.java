package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.BandejaEventos;
import com.admaal.resolvo.domain.EventoTicket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Imita la bandeja de Postgres: un evento reclamado no vuelve a salir hasta que vence el reclamo. */
public final class BandejaEnMemoria implements BandejaEventos {

  public enum Estado {
    PENDIENTE,
    HECHO,
    FALLIDO
  }

  private static final class Fila {
    final EventoTicket evento;
    int intentos;
    Estado estado = Estado.PENDIENTE;
    boolean reclamado;
    String error;

    Fila(EventoTicket evento) {
      this.evento = evento;
    }
  }

  private final Map<UUID, Fila> filas = new LinkedHashMap<>();

  public void agregar(EventoTicket evento) {
    filas.put(evento.id(), new Fila(evento));
  }

  /** Simula la entrega repetida del mismo evento: vuelve a estar pendiente, con su mismo id. */
  public void reentregar(UUID id) {
    Fila fila = filas.get(id);
    fila.estado = Estado.PENDIENTE;
    fila.reclamado = false;
  }

  /** Simula que pasa el tiempo del reclamo: los pendientes vuelven a poder reclamarse. */
  public void vencerReclamos() {
    filas.values().forEach(fila -> fila.reclamado = false);
  }

  public Estado estadoDe(UUID id) {
    return filas.get(id).estado;
  }

  public int intentosDe(UUID id) {
    return filas.get(id).intentos;
  }

  public String errorDe(UUID id) {
    return filas.get(id).error;
  }

  @Override
  public List<Reclamado> reclamar(int maximo) {
    List<Reclamado> lote = new ArrayList<>();
    for (Fila fila : filas.values()) {
      if (lote.size() >= maximo) {
        break;
      }
      if (fila.estado == Estado.PENDIENTE && !fila.reclamado) {
        fila.reclamado = true;
        fila.intentos++;
        lote.add(new Reclamado(fila.evento, fila.intentos));
      }
    }
    return lote;
  }

  @Override
  public void marcarHecho(UUID eventoId) {
    filas.get(eventoId).estado = Estado.HECHO;
  }

  @Override
  public void fallar(UUID eventoId, String error, boolean definitivo) {
    Fila fila = filas.get(eventoId);
    fila.error = error;
    if (definitivo) {
      fila.estado = Estado.FALLIDO;
    }
  }
}
