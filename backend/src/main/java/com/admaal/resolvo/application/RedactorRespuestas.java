package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.Ticket;
import java.util.List;

public interface RedactorRespuestas {
  String modelo();

  BorradorConCitas redactar(Ticket ticket, List<Fragmento> fragmentos);
}
