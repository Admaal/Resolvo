package com.admaal.resolvo.application;

import com.admaal.resolvo.application.BandejaEventos.Reclamado;
import com.admaal.resolvo.domain.EventoTicket;
import java.util.List;

/**
 * Atiende la bandeja de eventos. Cada evento llega a cada consumidor que lo admite una sola vez,
 * aunque se entregue repetido: el consumidor se anota como procesado al terminar. Un fallo deja el
 * evento para reintentar y, al agotar los intentos, lo da por fallido sin parar a los demas.
 */
public final class ProcesarEventos {

  public record Resultado(int hechos, int reintentables, int fallidos) {}

  private final BandejaEventos bandeja;
  private final EventosProcesados procesados;
  private final List<ConsumidorEventos> consumidores;
  private final int maxIntentos;
  private final int tamanoLote;

  public ProcesarEventos(
      BandejaEventos bandeja,
      EventosProcesados procesados,
      List<ConsumidorEventos> consumidores,
      int maxIntentos,
      int tamanoLote) {
    this.bandeja = bandeja;
    this.procesados = procesados;
    this.consumidores = List.copyOf(consumidores);
    this.maxIntentos = maxIntentos;
    this.tamanoLote = tamanoLote;
  }

  /** Procesa lotes hasta que no queda nada que reclamar. */
  public Resultado ejecutar() {
    int hechos = 0;
    int reintentables = 0;
    int fallidos = 0;
    List<Reclamado> lote = bandeja.reclamar(tamanoLote);
    while (!lote.isEmpty()) {
      for (Reclamado reclamado : lote) {
        String error = atender(reclamado.evento());
        if (error == null) {
          bandeja.marcarHecho(reclamado.evento().id());
          hechos++;
        } else if (reclamado.intentos() >= maxIntentos) {
          bandeja.fallar(reclamado.evento().id(), error, true);
          fallidos++;
        } else {
          bandeja.fallar(reclamado.evento().id(), error, false);
          reintentables++;
        }
      }
      lote = bandeja.reclamar(tamanoLote);
    }
    return new Resultado(hechos, reintentables, fallidos);
  }

  /** Devuelve null si todos los consumidores terminaron, o el mensaje del primero que fallo. */
  private String atender(EventoTicket evento) {
    for (ConsumidorEventos consumidor : consumidores) {
      if (!consumidor.admite(evento.tipo())
          || procesados.yaProcesado(consumidor.nombre(), evento.id())) {
        continue;
      }
      try {
        consumidor.consumir(evento);
      } catch (RuntimeException fallo) {
        return consumidor.nombre() + ": " + fallo.getMessage();
      }
      procesados.registrar(consumidor.nombre(), evento.id());
    }
    return null;
  }
}
