package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.OrganizacionId;

/** Puerta del presupuesto diario de IA: se reserva antes de llamar al modelo y se libera al acabar. */
public interface Presupuesto {

  Presupuesto SIN_LIMITE = organizacion -> () -> {};

  /** Lanza {@link PresupuestoAgotado} si no cabe otra llamada hoy. */
  Reserva reservar(OrganizacionId organizacionId);

  interface Reserva extends AutoCloseable {
    @Override
    void close();
  }
}
