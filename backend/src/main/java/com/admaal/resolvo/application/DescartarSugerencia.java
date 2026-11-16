package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Sugerencia;
import com.admaal.resolvo.domain.SugerenciaId;
import com.admaal.resolvo.domain.UsuarioId;

public final class DescartarSugerencia {

  private final Sugerencias sugerencias;
  private final Reloj reloj;

  public DescartarSugerencia(Sugerencias sugerencias, Reloj reloj) {
    this.sugerencias = sugerencias;
    this.reloj = reloj;
  }

  public void ejecutar(
      OrganizacionId organizacionId, SugerenciaId sugerenciaId, UsuarioId actor, Rol rol) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    Sugerencia sugerencia =
        sugerencias.porId(organizacionId, sugerenciaId).orElseThrow(NoEncontrado::new);
    sugerencia.descartar(actor, rol, reloj.ahora());
    sugerencias.guardar(sugerencia);
  }
}
