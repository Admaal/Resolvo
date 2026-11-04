package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.Reloj;
import java.time.Duration;
import java.time.Instant;

public final class RelojManual implements Reloj {

  private Instant ahora;

  public RelojManual(Instant ahora) {
    this.ahora = ahora;
  }

  @Override
  public Instant ahora() {
    return ahora;
  }

  public void avanzar(Duration duracion) {
    ahora = ahora.plus(duracion);
  }
}
