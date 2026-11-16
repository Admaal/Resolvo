package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.Sugerencias;
import com.admaal.resolvo.domain.EstadoSugerencia;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Sugerencia;
import com.admaal.resolvo.domain.SugerenciaId;
import com.admaal.resolvo.domain.TicketId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SugerenciasEnMemoria implements Sugerencias {

  private final List<Sugerencia> guardadas = new ArrayList<>();
  private int vecesGuardado;

  @Override
  public void guardar(Sugerencia sugerencia) {
    vecesGuardado++;
    guardadas.removeIf(s -> s.id().equals(sugerencia.id()));
    guardadas.add(sugerencia);
  }

  @Override
  public Optional<Sugerencia> porId(OrganizacionId organizacionId, SugerenciaId id) {
    return guardadas.stream()
        .filter(s -> s.id().equals(id) && s.organizacionId().equals(organizacionId))
        .findFirst();
  }

  @Override
  public Optional<Sugerencia> pendienteDe(OrganizacionId organizacionId, TicketId ticketId) {
    return guardadas.stream()
        .filter(
            s ->
                s.organizacionId().equals(organizacionId)
                    && s.ticketId().equals(ticketId)
                    && s.estado() == EstadoSugerencia.PENDIENTE)
        .findFirst();
  }

  public int vecesGuardado() {
    return vecesGuardado;
  }

  public int total() {
    return guardadas.size();
  }
}
