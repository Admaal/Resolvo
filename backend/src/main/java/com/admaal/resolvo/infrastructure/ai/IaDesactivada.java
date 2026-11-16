package com.admaal.resolvo.infrastructure.ai;

import com.admaal.resolvo.application.BorradorConCitas;
import com.admaal.resolvo.application.Embebedor;
import com.admaal.resolvo.application.IaNoDisponible;
import com.admaal.resolvo.application.RedactorRespuestas;
import com.admaal.resolvo.domain.Embedding;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.Ticket;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Sin el perfil "gemini" no hay proveedor: las operaciones de IA fallan con un motivo claro. */
@Component
@Profile("!gemini")
public class IaDesactivada implements Embebedor, RedactorRespuestas {

  @Override
  public String modelo() {
    return "desactivada";
  }

  @Override
  public Embedding embeber(String texto) {
    throw new IaNoDisponible("ia-no-configurada");
  }

  @Override
  public BorradorConCitas redactar(Ticket ticket, List<Fragmento> fragmentos) {
    throw new IaNoDisponible("ia-no-configurada");
  }
}
