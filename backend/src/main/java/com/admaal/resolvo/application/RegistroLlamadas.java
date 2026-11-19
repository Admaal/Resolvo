package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Consumo;
import com.admaal.resolvo.domain.LlamadaIa;
import com.admaal.resolvo.domain.OrganizacionId;
import java.time.Instant;
import java.util.List;

public interface RegistroLlamadas {

  void registrar(LlamadaIa llamada);

  /** Las llamadas de una organizacion con {@code desde <= ocurridaEn < hasta}. */
  List<LlamadaIa> deLaOrganizacion(OrganizacionId organizacionId, Instant desde, Instant hasta);

  /** Llamadas y tokens de una organizacion con {@code desde <= ocurridaEn < hasta}, fallos incluidos. */
  Consumo consumoDe(OrganizacionId organizacionId, Instant desde, Instant hasta);
}
