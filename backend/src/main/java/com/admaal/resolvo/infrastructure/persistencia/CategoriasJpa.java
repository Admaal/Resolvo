package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.CatalogoDeCategorias;
import com.admaal.resolvo.application.Categorias;
import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.OrganizacionId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CategoriasJpa implements Categorias, CatalogoDeCategorias {

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

  @Override
  public List<Categoria> deLaOrganizacion(OrganizacionId organizacionId) {
    return categorias.findByOrganizacionId(organizacionId.valor()).stream()
        .map(Mapeo::categoria)
        .toList();
  }
}
