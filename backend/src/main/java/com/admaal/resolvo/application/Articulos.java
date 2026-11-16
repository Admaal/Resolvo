package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Articulo;
import com.admaal.resolvo.domain.OrganizacionId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Articulos {
  List<Articulo> deLaOrganizacion(OrganizacionId organizacionId);

  Optional<Articulo> porId(OrganizacionId organizacionId, UUID id);
}
