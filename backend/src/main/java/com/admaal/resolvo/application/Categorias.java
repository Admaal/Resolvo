package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.OrganizacionId;
import java.util.Optional;

public interface Categorias {
  Optional<Categoria> porId(OrganizacionId organizacionId, CategoriaId categoriaId);
}
