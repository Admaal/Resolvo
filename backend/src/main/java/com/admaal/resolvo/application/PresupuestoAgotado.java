package com.admaal.resolvo.application;

/** La organizacion ya gasto su presupuesto diario de IA. */
public final class PresupuestoAgotado extends RuntimeException {

  public PresupuestoAgotado() {
    super("ia-presupuesto-agotado");
  }
}
