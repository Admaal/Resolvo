package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.Articulos;
import com.admaal.resolvo.domain.Articulo;
import com.admaal.resolvo.domain.OrganizacionId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ArticulosEnMemoria implements Articulos {

  private final List<Articulo> articulos = new ArrayList<>();

  public void poner(Articulo articulo) {
    articulos.removeIf(a -> a.id().equals(articulo.id()));
    articulos.add(articulo);
  }

  @Override
  public List<Articulo> deLaOrganizacion(OrganizacionId organizacionId) {
    return articulos.stream().filter(a -> a.organizacionId().equals(organizacionId)).toList();
  }

  @Override
  public Optional<Articulo> porId(OrganizacionId organizacionId, UUID id) {
    return articulos.stream()
        .filter(a -> a.id().equals(id) && a.organizacionId().equals(organizacionId))
        .findFirst();
  }
}
