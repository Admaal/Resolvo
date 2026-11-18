package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Clasificacion;
import com.admaal.resolvo.domain.ClasificacionId;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.TicketId;
import java.util.Optional;

public interface Clasificaciones {
  void guardar(Clasificacion clasificacion);

  Optional<Clasificacion> porId(OrganizacionId organizacionId, ClasificacionId id);

  Optional<Clasificacion> pendienteDe(OrganizacionId organizacionId, TicketId ticketId);
}
