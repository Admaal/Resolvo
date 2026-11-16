package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.BorradorConCitas;
import com.admaal.resolvo.application.RedactorRespuestas;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.FragmentoId;
import com.admaal.resolvo.domain.Ticket;
import java.util.List;

public final class RedactorFalso implements RedactorRespuestas {

  private int llamadas;
  private List<Fragmento> recibidos = List.of();
  private List<FragmentoId> citasForzadas;
  private RuntimeException fallo;

  @Override
  public String modelo() {
    return "redactor-falso";
  }

  @Override
  public BorradorConCitas redactar(Ticket ticket, List<Fragmento> fragmentos) {
    llamadas++;
    recibidos = List.copyOf(fragmentos);
    if (fallo != null) {
      throw fallo;
    }
    List<FragmentoId> citas =
        citasForzadas != null ? citasForzadas : List.of(fragmentos.get(0).id());
    return new BorradorConCitas("Borrador sugerido", citas);
  }

  public RedactorFalso citando(List<FragmentoId> citas) {
    this.citasForzadas = citas;
    return this;
  }

  public RedactorFalso fallando(RuntimeException error) {
    this.fallo = error;
    return this;
  }

  public int llamadas() {
    return llamadas;
  }

  public List<Fragmento> recibidos() {
    return recibidos;
  }

  public void reiniciar() {
    llamadas = 0;
    recibidos = List.of();
    citasForzadas = null;
    fallo = null;
  }
}
