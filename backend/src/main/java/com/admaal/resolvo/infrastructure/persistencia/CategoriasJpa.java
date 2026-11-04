package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.Categorias;
import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.OrganizacionId;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CategoriasJpa implements Categorias {

  private final CategoriaJpa categorias;

  public CategoriasJpa(CategoriaJpa categorias) {
    this.categorias = categorias;
  }

  @Override
  public Optional<Categoria> porId(OrganizacionId organizacionId, CategoriaId categoriaId) {
    return categorias
        .findById(categoriaId.valor())
        .filter(fila -> fila.getOrganizacionId().equals(organizacionId.valor()))
        .map(Mapeo::categoria);
  }
}
