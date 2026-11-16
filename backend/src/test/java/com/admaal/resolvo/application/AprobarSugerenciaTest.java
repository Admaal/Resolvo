package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.Banco;
import com.admaal.resolvo.application.soporte.RelojManual;
import com.admaal.resolvo.application.soporte.RepositorioEnMemoria;
import com.admaal.resolvo.application.soporte.SugerenciasEnMemoria;
import com.admaal.resolvo.domain.Comentario;
import com.admaal.resolvo.domain.EstadoSugerencia;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Sugerencia;
import com.admaal.resolvo.domain.SugerenciaId;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TransicionNoPermitida;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AprobarSugerenciaTest {

  private static final Instant AHORA = Instant.parse("2026-11-16T10:00:00Z");

  private final OrganizacionId acme = new OrganizacionId(UUID.randomUUID());
  private final OrganizacionId otra = new OrganizacionId(UUID.randomUUID());
  private final UsuarioId agente = new UsuarioId(UUID.randomUUID());
  private final RepositorioEnMemoria tickets = new RepositorioEnMemoria();
  private final SugerenciasEnMemoria sugerencias = new SugerenciasEnMemoria();
  private final Ticket ticket =
      Banco.abrir(tickets, acme, new UsuarioId(UUID.randomUUID()), Prioridad.ALTA, AHORA);
  private final Sugerencia sugerencia =
      Sugerencia.crear(
          SugerenciaId.nuevo(), acme, ticket.id(), "Borrador", "m", AHORA, List.of(), Set.of());
  private final AprobarSugerencia caso =
      new AprobarSugerencia(sugerencias, tickets, new RelojManual(AHORA));

  AprobarSugerenciaTest() {
    sugerencias.guardar(sugerencia);
  }

  @Test
  void publicaUnComentarioPublicoVisibleAlCliente() {
    Comentario comentario = caso.ejecutar(acme, sugerencia.id(), agente, Rol.AGENTE, "Texto final");

    assertThat(comentario.interno()).isFalse();
    assertThat(comentario.cuerpo()).isEqualTo("Texto final");
    assertThat(comentario.autorId()).isEqualTo(agente);
    assertThat(ticket.visiblesPara(Rol.CLIENTE)).containsExactly(comentario);
    assertThat(sugerencia.estado()).isEqualTo(EstadoSugerencia.APROBADA);
  }

  @Test
  void segundaAprobacionNoDuplicaComentario() {
    caso.ejecutar(acme, sugerencia.id(), agente, Rol.AGENTE, "Texto final");

    assertThatThrownBy(() -> caso.ejecutar(acme, sugerencia.id(), agente, Rol.AGENTE, "Otra vez"))
        .isInstanceOf(TransicionNoPermitida.class);

    assertThat(ticket.visiblesPara(Rol.AGENTE)).hasSize(1);
  }

  @Test
  void sugerenciaDeOtraOrgEs404() {
    assertThatThrownBy(() -> caso.ejecutar(otra, sugerencia.id(), agente, Rol.AGENTE, "x"))
        .isInstanceOf(NoEncontrado.class);

    assertThat(ticket.visiblesPara(Rol.AGENTE)).isEmpty();
  }

  @Test
  void clienteRecibe403YNoPublicaNada() {
    assertThatThrownBy(() -> caso.ejecutar(acme, sugerencia.id(), agente, Rol.CLIENTE, "x"))
        .isInstanceOf(Prohibido.class);

    assertThat(ticket.visiblesPara(Rol.AGENTE)).isEmpty();
    assertThat(sugerencia.estado()).isEqualTo(EstadoSugerencia.PENDIENTE);
  }

  @Test
  void textoInvalidoNoPublicaNiResuelve() {
    assertThatThrownBy(() -> caso.ejecutar(acme, sugerencia.id(), agente, Rol.AGENTE, "  "))
        .isInstanceOf(RuntimeException.class);

    assertThat(ticket.visiblesPara(Rol.AGENTE)).isEmpty();
    assertThat(sugerencia.estado()).isEqualTo(EstadoSugerencia.PENDIENTE);
  }

  @Test
  void guardaElTicketYLaSugerenciaResuelta() {
    int ticketsAntes = tickets.vecesGuardado();
    int sugerenciasAntes = sugerencias.vecesGuardado();

    caso.ejecutar(acme, sugerencia.id(), agente, Rol.AGENTE, "Texto final");

    assertThat(tickets.vecesGuardado()).isEqualTo(ticketsAntes + 1);
    assertThat(sugerencias.vecesGuardado()).isEqualTo(sugerenciasAntes + 1);
  }
}
