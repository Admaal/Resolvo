package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.OrganizacionId;
import java.util.List;

public interface CatalogoDeCategorias {
  List<Categoria> deLaOrganizacion(OrganizacionId organizacionId);
}
