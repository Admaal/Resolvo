package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Embedding;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.FuenteConocimiento;
import com.admaal.resolvo.domain.OrganizacionId;
import java.util.List;
import java.util.UUID;

public interface BaseConocimiento {

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

  List<Fragmento> buscar(
      OrganizacionId organizacionId, Embedding consulta, int k, UUID excluirFuenteId);
}
