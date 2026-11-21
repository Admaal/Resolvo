package com.admaal.resolvo.domain;

import java.time.Instant;

/**
 * Un comentario de un ticket. {@code moderacion} es nula mientras nadie lo haya ocultado; con ella,
 * el comentario está retirado pero conserva su texto para el equipo. {@code cuerpo} es nulo solo en
 * la copia que ve un cliente de un comentario retirado.
 */
public record Comentario(
    ComentarioId id,
    UsuarioId autorId,
    String cuerpo,
    boolean interno,
    Instant creadoEn,
    Moderacion moderacion) {

  public Comentario(
      ComentarioId id, UsuarioId autorId, String cuerpo, boolean interno, Instant creadoEn) {
    this(id, autorId, cuerpo, interno, creadoEn, null);
  }

  public boolean retirado() {
    return moderacion != null;
  }

  Comentario moderado(Moderacion nueva) {
    return new Comentario(id, autorId, cuerpo, interno, creadoEn, nueva);
  }

  /** La copia para un cliente: sin texto y sin saber quién lo retiró ni por qué. */
  Comentario sinCuerpo() {
    return new Comentario(id, autorId, null, interno, creadoEn, new Moderacion(null, null, null));
  }
}
