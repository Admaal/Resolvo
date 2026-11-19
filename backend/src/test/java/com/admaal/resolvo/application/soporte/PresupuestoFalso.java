package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.Presupuesto;
import com.admaal.resolvo.application.PresupuestoAgotado;
import com.admaal.resolvo.domain.OrganizacionId;

/** Presupuesto de pruebas: se puede agotar y cuenta las reservas hechas y las que siguen abiertas. */
public final class PresupuestoFalso implements Presupuesto {

  private boolean agotado;
  private int reservas;
  private int abiertas;

  public void agotar() {
    agotado = true;
  }

  public int reservas() {
    return reservas;
  }

  public int abiertas() {
    return abiertas;
  }

  @Override
  public Reserva reservar(OrganizacionId organizacionId) {
    if (agotado) {
      throw new PresupuestoAgotado();
    }
    reservas++;
    abiertas++;
    return () -> abiertas--;
  }
}
