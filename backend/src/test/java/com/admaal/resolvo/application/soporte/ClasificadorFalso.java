package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.ClasificacionPropuesta;
import com.admaal.resolvo.application.ClasificadorTickets;
import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Ticket;
import java.util.List;

public final class ClasificadorFalso implements ClasificadorTickets {

  private int llamadas;
  private List<Categoria> recibidas = List.of();
  private ClasificacionPropuesta respuesta = new ClasificacionPropuesta("Incidencia", Prioridad.ALTA, "motivo");
  private RuntimeException fallo;

  @Override
  public String modelo() {
    return "clasificador-falso";
  }

  @Override
  public ClasificacionPropuesta clasificar(Ticket ticket, List<Categoria> categorias) {
    llamadas++;
    recibidas = List.copyOf(categorias);
    if (fallo != null) {
      throw fallo;
    }
    return respuesta;
  }

  public ClasificadorFalso respondiendo(String categoria, Prioridad prioridad, String motivo) {
    this.respuesta = new ClasificacionPropuesta(categoria, prioridad, motivo);
    return this;
  }

  public ClasificadorFalso fallando(RuntimeException error) {
    this.fallo = error;
    return this;
  }

  public void reiniciar() {
    llamadas = 0;
    recibidas = List.of();
    respuesta = new ClasificacionPropuesta("Incidencia", Prioridad.ALTA, "motivo");
    fallo = null;
  }

  public int llamadas() {
    return llamadas;
  }

  public List<Categoria> recibidas() {
    return recibidas;
  }
}
