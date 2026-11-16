package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.Clasificaciones;
import com.admaal.resolvo.domain.Clasificacion;
import com.admaal.resolvo.domain.ClasificacionId;
import com.admaal.resolvo.domain.EstadoClasificacion;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.TicketId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ClasificacionesEnMemoria implements Clasificaciones {

  private final List<Clasificacion> guardadas = new ArrayList<>();
  private int vecesGuardado;

  @Override
  public void guardar(Clasificacion clasificacion) {
    vecesGuardado++;
    guardadas.removeIf(c -> c.id().equals(clasificacion.id()));
    guardadas.add(clasificacion);
  }

  @Override
  public Optional<Clasificacion> porId(OrganizacionId organizacionId, ClasificacionId id) {
    return guardadas.stream()
        .filter(c -> c.id().equals(id) && c.organizacionId().equals(organizacionId))
        .findFirst();
  }

  @Override
  public Optional<Clasificacion> pendienteDe(OrganizacionId organizacionId, TicketId ticketId) {
    return guardadas.stream()
        .filter(
            c ->
                c.organizacionId().equals(organizacionId)
                    && c.ticketId().equals(ticketId)
                    && c.estado() == EstadoClasificacion.PENDIENTE)
        .findFirst();
  }

  public int vecesGuardado() {
    return vecesGuardado;
  }

  public int total() {
    return guardadas.size();
  }
}
