package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.Banco;
import com.admaal.resolvo.application.soporte.RepositorioEnMemoria;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarTicketTest {

  @Test
  void otraOrganizacionEsIndistinguibleDeNoExistir() {
    RepositorioEnMemoria tickets = new RepositorioEnMemoria();
    OrganizacionId organizacion = new OrganizacionId(UUID.randomUUID());
    Ticket ticket =
        Banco.abrir(
            tickets,
            organizacion,
            new UsuarioId(UUID.randomUUID()),
            Prioridad.MEDIA,
            Instant.parse("2026-11-04T10:00:00Z"));
    ConsultarTicket caso = new ConsultarTicket(tickets);

    assertThatThrownBy(
            () -> caso.ejecutar(new OrganizacionId(UUID.randomUUID()), ticket.id()))
        .isInstanceOf(NoEncontrado.class);
    assertThatThrownBy(() -> caso.ejecutar(organizacion, TicketId.nuevo()))
        .isInstanceOf(NoEncontrado.class);
  }
}
