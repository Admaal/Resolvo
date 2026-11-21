package com.admaal.resolvo.application;

import java.time.Instant;

/** Limpieza de lo que escriben los visitantes de la demo publica. La semilla no se toca. */
public interface ComentariosDemo {

  /** Borra los comentarios de visitantes creados antes del limite; devuelve cuantos. */
  int borrarCreadosAntesDe(Instant limite);
}
