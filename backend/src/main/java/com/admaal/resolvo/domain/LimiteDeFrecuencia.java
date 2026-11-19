package com.admaal.resolvo.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Limite de peticiones por clave con ventana deslizante, en memoria. Vale para una sola
 * instancia de la API (Render Free corre una). El tiempo lo pone quien llama.
 */
public final class LimiteDeFrecuencia {

  /** Con mas claves que esto, cada escritura purga las caducadas. */
  private static final int CLAVES_ANTES_DE_PURGAR = 1000;

  public record Decision(boolean admitida, Duration reintentarEn) {}

  private final int maximo;
  private final Duration ventana;
  private final Map<String, ArrayDeque<Instant>> marcas = new HashMap<>();

  public LimiteDeFrecuencia(int maximo, Duration ventana) {
    if (maximo < 1 || ventana.isZero() || ventana.isNegative()) {
      throw new IllegalArgumentException("maximo y ventana deben ser positivos");
    }
    this.maximo = maximo;
    this.ventana = ventana;
  }

  /** Mira si cabe otra peticion, sin contarla. */
  public synchronized Decision comprobar(String clave, Instant ahora) {
    ArrayDeque<Instant> vigentes = vigentes(clave, ahora);
    if (vigentes == null || vigentes.size() < maximo) {
      return new Decision(true, Duration.ZERO);
    }
    return new Decision(false, ventana.minus(Duration.between(vigentes.peekFirst(), ahora)));
  }

  /** Cuenta una peticion sin mirar el limite (por ejemplo, un intento fallido). */
  public synchronized void registrar(String clave, Instant ahora) {
    if (marcas.size() > CLAVES_ANTES_DE_PURGAR) {
      purgar(ahora);
    }
    ArrayDeque<Instant> vigentes = vigentes(clave, ahora);
    if (vigentes == null) {
      vigentes = new ArrayDeque<>();
      marcas.put(clave, vigentes);
    }
    vigentes.addLast(ahora);
  }

  /** Mira y cuenta en un solo paso: un rechazo no consume cupo. */
  public synchronized Decision admitir(String clave, Instant ahora) {
    Decision decision = comprobar(clave, ahora);
    if (decision.admitida()) {
      registrar(clave, ahora);
    }
    return decision;
  }

  public synchronized void reiniciar(String clave) {
    marcas.remove(clave);
  }

  /** Olvida las claves sin ninguna marca dentro de la ventana. */
  public synchronized void purgar(Instant ahora) {
    marcas.values().removeIf(lista -> recortar(lista, ahora).isEmpty());
  }

  public synchronized int clavesRecordadas() {
    return marcas.size();
  }

  /** Las marcas de la clave que siguen dentro de la ventana, o null si no queda ninguna. */
  private ArrayDeque<Instant> vigentes(String clave, Instant ahora) {
    ArrayDeque<Instant> lista = marcas.get(clave);
    if (lista == null) {
      return null;
    }
    if (recortar(lista, ahora).isEmpty()) {
      marcas.remove(clave);
      return null;
    }
    return lista;
  }

  private ArrayDeque<Instant> recortar(ArrayDeque<Instant> lista, Instant ahora) {
    while (!lista.isEmpty() && !Duration.between(lista.peekFirst(), ahora).minus(ventana).isNegative()) {
      lista.removeFirst();
    }
    return lista;
  }
}
