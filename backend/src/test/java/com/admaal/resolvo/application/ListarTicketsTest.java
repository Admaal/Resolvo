package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.application.soporte.Banco;
import com.admaal.resolvo.application.soporte.RepositorioEnMemoria;
import com.admaal.resolvo.application.soporte.UsuariosEnMemoria;
import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.Usuario;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarTicketsTest {

  private final Instant ahora = Instant.parse("2026-11-04T10:00:00Z");
  private final OrganizacionId organizacion = new OrganizacionId(UUID.randomUUID());
  private final UsuarioId autor = new UsuarioId(UUID.randomUUID());

  @Test
  void filtraPorEstadoPrioridadYAsignado() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    UsuariosEnMemoria usuarios = new UsuariosEnMemoria();
    Usuario agente =
        Usuario.nuevo(new UsuarioId(UUID.randomUUID()), organizacion, "agente@acme.test", Rol.AGENTE, "hash");
    usuarios.guardar(agente);
    Ticket media = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);
    Ticket alta = Banco.abrir(tickets, organizacion, autor, Prioridad.ALTA, ahora);
    Ticket resuelto = Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);
    Banco.abrir(tickets, new OrganizacionId(UUID.randomUUID()), autor, Prioridad.MEDIA, ahora);
    new AsignarTicket(tickets, usuarios).ejecutar(organizacion, alta.id(), agente.id(), Rol.AGENTE);
    new AsignarTicket(tickets, usuarios)
        .ejecutar(organizacion, resuelto.id(), agente.id(), Rol.AGENTE);
    new TransicionarTicket(tickets)
        .ejecutar(organizacion, resuelto.id(), EstadoTicket.RESUELTO, Rol.AGENTE);

    Pagina<Ticket> pagina =
        new ListarTickets(tickets)
            .ejecutar(
                new ConsultaTickets(
                    organizacion, EstadoTicket.ABIERTO, Prioridad.ALTA, agente.id(), 0, 20));

    assertThat(pagina.elementos()).extracting(Ticket::id).containsExactly(alta.id());
    assertThat(pagina.total()).isEqualTo(1);
    assertThat(media.estado()).isEqualTo(EstadoTicket.ABIERTO);
  }

  @Test
  void acotaElTamanoYLaPagina() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    Banco.abrir(tickets, organizacion, autor, Prioridad.BAJA, ahora);
    Banco.abrir(tickets, organizacion, autor, Prioridad.MEDIA, ahora);
    Banco.abrir(tickets, organizacion, autor, Prioridad.ALTA, ahora);
    ListarTickets caso = new ListarTickets(tickets);

    Pagina<Ticket> uno =
        caso.ejecutar(new ConsultaTickets(organizacion, null, null, null, 0, 1));
    assertThat(uno.elementos()).hasSize(1);
    assertThat(uno.tamano()).isEqualTo(1);

    Pagina<Ticket> segunda =
        caso.ejecutar(new ConsultaTickets(organizacion, null, null, null, 1, 2));
    assertThat(segunda.elementos()).hasSize(1);
    assertThat(segunda.total()).isEqualTo(3);
    assertThat(segunda.pagina()).isEqualTo(1);

    Pagina<Ticket> tope =
        caso.ejecutar(new ConsultaTickets(organizacion, null, null, null, 0, 51));
    assertThat(tope.tamano()).isEqualTo(50);
    assertThat(tope.elementos()).hasSize(3);

    Pagina<Ticket> defecto =
        caso.ejecutar(new ConsultaTickets(organizacion, null, null, null, -1, 0));
    assertThat(defecto.pagina()).isZero();
    assertThat(defecto.tamano()).isEqualTo(20);
    assertThat(defecto.elementos()).hasSize(3);
  }
}
