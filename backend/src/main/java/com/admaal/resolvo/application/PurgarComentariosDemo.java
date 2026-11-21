package com.admaal.resolvo.application;

import java.time.Duration;

/** Quita de la demo publica los comentarios de visitantes de mas de 24 horas. */
public final class PurgarComentariosDemo {

  static final Duration VIDA = Duration.ofHours(24);

  private final ComentariosDemo comentarios;
  private final Reloj reloj;

  public PurgarComentariosDemo(ComentariosDemo comentarios, Reloj reloj) {
    this.comentarios = comentarios;
    this.reloj = reloj;
  }

  public int ejecutar() {
    return comentarios.borrarCreadosAntesDe(reloj.ahora().minus(VIDA));
  }
}
