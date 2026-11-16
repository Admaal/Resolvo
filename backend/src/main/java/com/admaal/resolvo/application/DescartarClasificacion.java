package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Clasificacion;
import com.admaal.resolvo.domain.ClasificacionId;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.UsuarioId;

public final class DescartarClasificacion {

  private final Clasificaciones clasificaciones;
  private final Reloj reloj;

  public DescartarClasificacion(Clasificaciones clasificaciones, Reloj reloj) {
    this.clasificaciones = clasificaciones;
    this.reloj = reloj;
  }

  public void ejecutar(
      OrganizacionId organizacionId, ClasificacionId clasificacionId, UsuarioId actor, Rol rol) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    Clasificacion clasificacion =
        clasificaciones.porId(organizacionId, clasificacionId).orElseThrow(NoEncontrado::new);
    clasificacion.descartar(actor, rol, reloj.ahora());
    clasificaciones.guardar(clasificacion);
  }
}
