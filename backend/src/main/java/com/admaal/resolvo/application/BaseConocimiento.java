package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Embedding;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.FuenteConocimiento;
import com.admaal.resolvo.domain.OrganizacionId;
import java.util.List;
import java.util.UUID;

public interface BaseConocimiento {

  /** La distancia coseno va de 0 (identico) a 2 (opuesto): con 2 no se descarta nada. */
  double SIN_LIMITE = 2.0;

  boolean estaIndexada(
      OrganizacionId organizacionId,
      FuenteConocimiento fuente,
      UUID fuenteId,
      String hashFuente,
      String modelo);

  void reemplazar(
      OrganizacionId organizacionId,
      FuenteConocimiento fuente,
      UUID fuenteId,
      String hashFuente,
      String modelo,
      List<FragmentoIndexado> fragmentos);

  default List<Fragmento> buscar(
      OrganizacionId organizacionId, Embedding consulta, int k, UUID excluirFuenteId) {
    return buscar(organizacionId, consulta, k, excluirFuenteId, SIN_LIMITE);
  }

  /** Los {@code k} fragmentos mas cercanos cuya distancia coseno no supera {@code distanciaMaxima}. */
  List<Fragmento> buscar(
      OrganizacionId organizacionId,
      Embedding consulta,
      int k,
      UUID excluirFuenteId,
      double distanciaMaxima);
}
