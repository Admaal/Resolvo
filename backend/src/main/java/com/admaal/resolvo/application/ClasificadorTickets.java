package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.Ticket;
import java.util.List;

public interface ClasificadorTickets {
  String modelo();

  ClasificacionPropuesta clasificar(Ticket ticket, List<Categoria> categorias);
}
