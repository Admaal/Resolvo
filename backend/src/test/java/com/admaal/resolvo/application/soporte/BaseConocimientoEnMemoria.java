package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.BaseConocimiento;
import com.admaal.resolvo.application.FragmentoIndexado;
import com.admaal.resolvo.domain.Embedding;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.FragmentoId;
import com.admaal.resolvo.domain.FuenteConocimiento;
import com.admaal.resolvo.domain.OrganizacionId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BaseConocimientoEnMemoria implements BaseConocimiento {

  private record Entrada(UUID fuenteId, String hashFuente, String modelo, Fragmento fragmento) {}

  private final List<Entrada> entradas = new ArrayList<>();

  @Override
  public boolean estaIndexada(
      OrganizacionId organizacionId,
      FuenteConocimiento fuente,
      UUID fuenteId,
      String hashFuente,
      String modelo) {
    return entradas.stream()
        .anyMatch(
            e ->
                e.fragmento().organizacionId().equals(organizacionId)
                    && e.fuenteId().equals(fuenteId)
                    && e.fragmento().fuente() == fuente
                    && e.hashFuente().equals(hashFuente)
                    && e.modelo().equals(modelo));
  }

  @Override
  public void reemplazar(
      OrganizacionId organizacionId,
      FuenteConocimiento fuente,
      UUID fuenteId,
      String hashFuente,
      String modelo,
      List<FragmentoIndexado> fragmentos) {
    entradas.removeIf(
        e ->
            e.fragmento().organizacionId().equals(organizacionId)
                && e.fuenteId().equals(fuenteId)
                && e.fragmento().fuente() == fuente);
    for (FragmentoIndexado indexado : fragmentos) {
      entradas.add(new Entrada(fuenteId, hashFuente, modelo, indexado.fragmento()));
    }
  }

  @Override
  public List<Fragmento> buscar(
      OrganizacionId organizacionId, Embedding consulta, int k, UUID excluirFuenteId) {
    return entradas.stream()
        .map(Entrada::fragmento)
        .filter(f -> f.organizacionId().equals(organizacionId))
        .filter(f -> !f.fuenteId().equals(excluirFuenteId))
        .limit(k)
        .toList();
  }

  public Fragmento sembrar(OrganizacionId organizacion, UUID fuenteId, String titulo, String texto) {
    Fragmento fragmento =
        new Fragmento(
            FragmentoId.nuevo(), organizacion, FuenteConocimiento.ARTICULO, fuenteId, titulo, texto);
    entradas.add(new Entrada(fuenteId, Fragmento.hashDe(texto), "falso-768", fragmento));
    return fragmento;
  }

  public List<Fragmento> fragmentosDe(UUID fuenteId) {
    return entradas.stream()
        .filter(e -> e.fuenteId().equals(fuenteId))
        .map(Entrada::fragmento)
        .toList();
  }

  public int total() {
    return entradas.size();
  }
}
