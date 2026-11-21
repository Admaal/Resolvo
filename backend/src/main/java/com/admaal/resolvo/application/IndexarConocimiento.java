package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Articulo;
import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.FragmentoId;
import com.admaal.resolvo.domain.FuenteConocimiento;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.Troceador;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class IndexarConocimiento {

  private final Embebedor embebedor;
  private final BaseConocimiento base;
  private final Articulos articulos;
  private final TicketRepositorio tickets;

  public IndexarConocimiento(
      Embebedor embebedor, BaseConocimiento base, Articulos articulos, TicketRepositorio tickets) {
    this.embebedor = embebedor;
    this.base = base;
    this.articulos = articulos;
    this.tickets = tickets;
  }

  public void organizacion(OrganizacionId organizacionId) {
    articulos.deLaOrganizacion(organizacionId).forEach(this::articulo);
    tickets.deLaOrganizacion(organizacionId).forEach(this::ticket);
  }

  public void articulo(Articulo articulo) {
    indexar(
        FuenteConocimiento.ARTICULO,
        articulo.id(),
        articulo.organizacionId(),
        articulo.titulo(),
        articulo.titulo() + "\n" + articulo.cuerpo());
  }

  public void ticket(Ticket ticket) {
    if (ticket.estado() != EstadoTicket.RESUELTO && ticket.estado() != EstadoTicket.CERRADO) {
      return;
    }
    StringBuilder texto = new StringBuilder(ticket.asunto()).append('\n').append(ticket.cuerpo());
    ticket.visiblesPara(Rol.CLIENTE).stream()
        .filter(c -> !c.retirado())
        .forEach(c -> texto.append('\n').append(c.cuerpo()));
    indexar(
        FuenteConocimiento.TICKET,
        ticket.id().valor(),
        ticket.organizacionId(),
        ticket.codigo() + " " + ticket.asunto(),
        texto.toString());
  }

  private void indexar(
      FuenteConocimiento fuente,
      UUID fuenteId,
      OrganizacionId organizacionId,
      String titulo,
      String texto) {
    String hashFuente = Fragmento.hashDe(texto);
    String modelo = embebedor.modelo();
    if (base.estaIndexada(organizacionId, fuente, fuenteId, hashFuente, modelo)) {
      return;
    }
    List<FragmentoIndexado> fragmentos = new ArrayList<>();
    for (String trozo : Troceador.trocear(texto)) {
      Fragmento fragmento =
          new Fragmento(FragmentoId.nuevo(), organizacionId, fuente, fuenteId, titulo, trozo);
      fragmentos.add(new FragmentoIndexado(fragmento, embebedor.embeber(trozo)));
    }
    base.reemplazar(organizacionId, fuente, fuenteId, hashFuente, modelo, fragmentos);
  }
}
