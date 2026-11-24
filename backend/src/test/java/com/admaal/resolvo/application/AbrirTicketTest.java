package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AbrirTicketTest {

  private final Instant ahora = Instant.parse("2026-11-04T10:00:00Z");
  private final OrganizacionId organizacion = new OrganizacionId(UUID.randomUUID());
  private final CategoriaId categoriaId = new CategoriaId(UUID.randomUUID());

  @Test
  void naceEnEstadoAbierto() {
    var tickets = new TicketsEnMemoria();
    var caso = caso(tickets, "RS-1", "Incidencia");

    var ticket =
        caso.ejecutar(
            comando(Prioridad.MEDIA, "La impresora no responde", "Desde esta mañana"));

    assertThat(ticket.estado()).isEqualTo(EstadoTicket.ABIERTO);
    assertThat(ticket.id()).isNotNull();
    assertThat(ticket.abiertoEn()).isEqualTo(ahora);
    assertThat(ticket.asunto()).isEqualTo("La impresora no responde");
    assertThat(ticket.cuerpo()).isEqualTo("Desde esta mañana");
    assertThat(ticket.prioridad()).isEqualTo(Prioridad.MEDIA);
    assertThat(ticket.autorId()).isNotNull();
    assertThat(ticket.categoriaId()).isEqualTo(categoriaId);
    assertThat(ticket.organizacionId()).isEqualTo(organizacion);
    assertThat(tickets.unico().estado()).isEqualTo(EstadoTicket.ABIERTO);
  }

  @Test
  void calculaElVencimientoSegunCategoriaYPrioridad() {
    assertVence("Incidencia", Prioridad.URGENTE, 1);
    assertVence("Incidencia", Prioridad.ALTA, 4);
    assertVence("Incidencia", Prioridad.MEDIA, 8);
    assertVence("Incidencia", Prioridad.BAJA, 24);
    assertVence("Consulta", Prioridad.URGENTE, 2);
    assertVence("Consulta", Prioridad.ALTA, 8);
    assertVence("Consulta", Prioridad.MEDIA, 24);
    assertVence("Consulta", Prioridad.BAJA, 48);
  }

  private void assertVence(String categoria, Prioridad prioridad, int horas) {
    var ticket =
        caso(new TicketsEnMemoria(), "RS-1", categoria)
            .ejecutar(comando(prioridad, "Asunto valido", "Cuerpo valido"));
    assertThat(ticket.venceEl()).isEqualTo(ahora.plus(Duration.ofHours(horas)));
  }

  @Test
  void recibeCodigoPublico() {
    var ticket =
        caso(new TicketsEnMemoria(), "RS-15", "Consulta")
            .ejecutar(comando(Prioridad.BAJA, "Asunto valido", "Cuerpo valido"));

    assertThat(ticket.codigo()).isEqualTo("RS-15");
  }

  private AbrirTicket caso(TicketsEnMemoria tickets, String codigo, String categoria) {
    return new AbrirTicket(
        () -> ahora,
        organizacionId -> codigo,
        tickets,
        (org, id) -> Optional.of(new Categoria(id, org, categoria)));
  }

  private AbrirTicketComando comando(Prioridad prioridad, String asunto, String cuerpo) {
    return new AbrirTicketComando(
        organizacion,
        new UsuarioId(UUID.randomUUID()),
        Rol.CLIENTE,
        categoriaId,
        prioridad,
        asunto,
        cuerpo);
  }

  private static final class TicketsEnMemoria implements TicketRepositorio {
    private final ArrayList<Ticket> guardados = new ArrayList<>();

    @Override
    public void guardar(Ticket ticket) {
      guardados.removeIf(existente -> existente.id().equals(ticket.id()));
      guardados.add(ticket);
    }

    @Override
    public Optional<Ticket> porId(OrganizacionId organizacionId, TicketId ticketId) {
      return guardados.stream()
          .filter(ticket -> ticket.id().equals(ticketId) && ticket.organizacionId().equals(organizacionId))
          .findFirst();
    }

    @Override
    public Optional<Ticket> porCodigo(OrganizacionId organizacionId, String codigo) {
      return guardados.stream()
          .filter(ticket -> ticket.codigo().equals(codigo) && ticket.organizacionId().equals(organizacionId))
          .findFirst();
    }

    @Override
    public List<Ticket> deLaOrganizacion(OrganizacionId organizacionId) {
      return guardados.stream()
          .filter(ticket -> ticket.organizacionId().equals(organizacionId))
          .toList();
    }

    private Ticket unico() {
      return guardados.get(0);
    }
  }
}
