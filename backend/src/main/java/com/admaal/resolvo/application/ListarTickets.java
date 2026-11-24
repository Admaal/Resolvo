package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Ticket;
import java.util.List;

public final class ListarTickets {

  private final TicketRepositorio tickets;

  public ListarTickets(TicketRepositorio tickets) {
    this.tickets = tickets;
  }

  public Pagina<Ticket> ejecutar(ConsultaTickets consulta) {
    int pedido = consulta.tamano();
    int tamano = pedido < 1 ? 20 : Math.min(pedido, 50);
    int pagina = Math.max(0, consulta.pagina());
    List<Ticket> filtrados =
        tickets.deLaOrganizacion(consulta.organizacionId()).stream()
            .filter(ticket -> ticket.visiblePara(consulta.solicitanteId(), consulta.rol()))
            .filter(
                ticket ->
                    ticket.coincide(consulta.estado(), consulta.prioridad(), consulta.asignadoId()))
            .toList();
    List<Ticket> ventana = new java.util.ArrayList<>();
    int desde = pagina * tamano;
    for (int indice = desde; indice < filtrados.size() && ventana.size() < tamano; indice++) {
      ventana.add(filtrados.get(indice));
    }
    return new Pagina<>(List.copyOf(ventana), pagina, tamano, filtrados.size());
  }
}
