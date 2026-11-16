package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Sugerencia;
import com.admaal.resolvo.domain.SugerenciaId;
import com.admaal.resolvo.domain.TicketId;
import java.util.Optional;

public interface Sugerencias {
  void guardar(Sugerencia sugerencia);

  Optional<Sugerencia> porId(OrganizacionId organizacionId, SugerenciaId id);

  Optional<Sugerencia> pendienteDe(OrganizacionId organizacionId, TicketId ticketId);
}
