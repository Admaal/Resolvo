package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.Banco;
import com.admaal.resolvo.application.soporte.ClasificacionesEnMemoria;
import com.admaal.resolvo.application.soporte.RelojManual;
import com.admaal.resolvo.application.soporte.RepositorioEnMemoria;
import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.Clasificacion;
import com.admaal.resolvo.domain.ClasificacionId;
import com.admaal.resolvo.domain.EstadoClasificacion;
import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.TextoInvalido;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TransicionNoPermitida;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AplicarClasificacionTest {

  private static final Instant AHORA = Instant.parse("2026-11-16T10:00:00Z");

  private final OrganizacionId acme = new OrganizacionId(UUID.randomUUID());
  private final OrganizacionId otra = new OrganizacionId(UUID.randomUUID());
  private final UsuarioId agente = new UsuarioId(UUID.randomUUID());
  private final Categoria incidencia = new Categoria(new CategoriaId(UUID.randomUUID()), acme, "Incidencia");
  private final Categoria consulta = new Categoria(new CategoriaId(UUID.randomUUID()), acme, "Consulta");
  private final Categoria ajena = new Categoria(new CategoriaId(UUID.randomUUID()), otra, "Incidencia");
  private final RepositorioEnMemoria tickets = new RepositorioEnMemoria();
  private final ClasificacionesEnMemoria clasificaciones = new ClasificacionesEnMemoria();
  private final Ticket ticket =
      Banco.abrir(tickets, acme, new UsuarioId(UUID.randomUUID()), Prioridad.BAJA, AHORA);
  private final Clasificacion propuesta =
      Clasificacion.proponer(
          ClasificacionId.nuevo(),
          acme,
          ticket.id(),
          incidencia.id(),
          Prioridad.ALTA,
          "motivo",
          "m",
          AHORA);
  private final AplicarClasificacion caso =
      new AplicarClasificacion(
          clasificaciones,
          tickets,
          (organizacion, id) ->
              List.of(incidencia, consulta, ajena).stream()
                  .filter(c -> c.id().equals(id) && c.organizacionId().equals(organizacion))
                  .findFirst(),
          new RelojManual(AHORA));

  AplicarClasificacionTest() {
    clasificaciones.guardar(propuesta);
  }

  private Ticket aplicar(CategoriaId categoria, Prioridad prioridad) {
    return caso.ejecutar(acme, propuesta.id(), categoria, prioridad, agente, Rol.AGENTE);
  }

  @Test
  void aplicaLoQueEligeElAgenteNoLoPropuesto() {
    Ticket resultado = aplicar(consulta.id(), Prioridad.URGENTE);

    assertThat(resultado.categoriaId()).isEqualTo(consulta.id());
    assertThat(resultado.prioridad()).isEqualTo(Prioridad.URGENTE);
    assertThat(tickets.porId(acme, ticket.id()).orElseThrow().categoriaId())
        .isEqualTo(consulta.id());
  }

  @Test
  void recalculaElSlaDesdeLaApertura() {
    Ticket resultado = aplicar(incidencia.id(), Prioridad.URGENTE);

    assertThat(resultado.venceEl()).isEqualTo(ticket.abiertoEn().plus(Duration.ofHours(1)));
  }

  @Test
  void marcaLaClasificacionComoAplicadaConQuienYCuando() {
    aplicar(incidencia.id(), Prioridad.ALTA);

    assertThat(propuesta.estado()).isEqualTo(EstadoClasificacion.APLICADA);
    assertThat(propuesta.resueltaPor()).isEqualTo(agente);
    assertThat(propuesta.resueltaEn()).isEqualTo(AHORA);
    assertThat(clasificaciones.pendienteDe(acme, ticket.id())).isEmpty();
  }

  @Test
  void categoriaDeOtraOrganizacionEsNoEncontrado() {
    assertThatThrownBy(() -> aplicar(ajena.id(), Prioridad.ALTA)).isInstanceOf(NoEncontrado.class);
    assertThat(propuesta.estado()).isEqualTo(EstadoClasificacion.PENDIENTE);
    assertThat(ticket.prioridad()).isEqualTo(Prioridad.BAJA);
  }

  @Test
  void clasificacionDeOtraOrganizacionEsNoEncontrado() {
    assertThatThrownBy(
            () ->
                caso.ejecutar(otra, propuesta.id(), ajena.id(), Prioridad.ALTA, agente, Rol.AGENTE))
        .isInstanceOf(NoEncontrado.class);
  }

  @Test
  void clienteRecibeProhibidoYNadaCambia() {
    assertThatThrownBy(
            () ->
                caso.ejecutar(
                    acme, propuesta.id(), incidencia.id(), Prioridad.ALTA, agente, Rol.CLIENTE))
        .isInstanceOf(Prohibido.class);
    assertThat(propuesta.estado()).isEqualTo(EstadoClasificacion.PENDIENTE);
    assertThat(ticket.prioridad()).isEqualTo(Prioridad.BAJA);
  }

  @Test
  void aplicarDosVecesEsTransicionNoPermitida() {
    aplicar(incidencia.id(), Prioridad.ALTA);

    assertThatThrownBy(() -> aplicar(incidencia.id(), Prioridad.ALTA))
        .isInstanceOf(TransicionNoPermitida.class);
  }

  @Test
  void unTicketResueltoEsTransicionNoPermitidaYLaClasificacionSigue() {
    ticket.transicionar(EstadoTicket.RESUELTO, Rol.AGENTE);

    assertThatThrownBy(() -> aplicar(incidencia.id(), Prioridad.ALTA))
        .isInstanceOf(TransicionNoPermitida.class);
    assertThat(propuesta.estado()).isEqualTo(EstadoClasificacion.PENDIENTE);
  }

  @Test
  void descartarNoTocaElTicket() {
    new DescartarClasificacion(clasificaciones, new RelojManual(AHORA))
        .ejecutar(acme, propuesta.id(), agente, Rol.AGENTE);

    assertThat(propuesta.estado()).isEqualTo(EstadoClasificacion.DESCARTADA);
    assertThat(ticket.prioridad()).isEqualTo(Prioridad.BAJA);
    assertThat(clasificaciones.pendienteDe(acme, ticket.id())).isEqualTo(Optional.empty());
  }

  @Test
  void descartarComoClienteEsProhibido() {
    DescartarClasificacion descartar =
        new DescartarClasificacion(clasificaciones, new RelojManual(AHORA));

    assertThatThrownBy(() -> descartar.ejecutar(acme, propuesta.id(), agente, Rol.CLIENTE))
        .isInstanceOf(Prohibido.class);
    assertThatThrownBy(() -> descartar.ejecutar(otra, propuesta.id(), agente, Rol.AGENTE))
        .isInstanceOf(NoEncontrado.class);
  }

  @Test
  void aplicarGuardaElTicketYLaClasificacionResuelta() {
    int ticketsAntes = tickets.vecesGuardado();
    int clasificacionesAntes = clasificaciones.vecesGuardado();

    aplicar(consulta.id(), Prioridad.ALTA);

    assertThat(tickets.vecesGuardado()).isEqualTo(ticketsAntes + 1);
    assertThat(clasificaciones.vecesGuardado()).isEqualTo(clasificacionesAntes + 1);
  }

  @Test
  void descartarGuardaLaClasificacionResuelta() {
    int antes = clasificaciones.vecesGuardado();

    new DescartarClasificacion(clasificaciones, new RelojManual(AHORA))
        .ejecutar(acme, propuesta.id(), agente, Rol.AGENTE);

    assertThat(clasificaciones.vecesGuardado()).isEqualTo(antes + 1);
  }

  @Test
  void unaAplicacionFallidaNoGuardaNada() {
    int ticketsAntes = tickets.vecesGuardado();
    int clasificacionesAntes = clasificaciones.vecesGuardado();

    assertThatThrownBy(() -> aplicar(ajena.id(), Prioridad.ALTA)).isInstanceOf(NoEncontrado.class);

    assertThat(tickets.vecesGuardado()).isEqualTo(ticketsAntes);
    assertThat(clasificaciones.vecesGuardado()).isEqualTo(clasificacionesAntes);
  }

  @Test
  void sinPrioridadONiCategoriaEsTextoInvalidoYNoCambiaNada() {
    int ticketsAntes = tickets.vecesGuardado();
    int clasificacionesAntes = clasificaciones.vecesGuardado();

    assertThatThrownBy(() -> aplicar(incidencia.id(), null)).isInstanceOf(TextoInvalido.class);
    assertThatThrownBy(() -> aplicar(null, Prioridad.ALTA)).isInstanceOf(TextoInvalido.class);

    assertThat(propuesta.estado()).isEqualTo(EstadoClasificacion.PENDIENTE);
    assertThat(ticket.prioridad()).isEqualTo(Prioridad.BAJA);
    assertThat(tickets.vecesGuardado()).isEqualTo(ticketsAntes);
    assertThat(clasificaciones.vecesGuardado()).isEqualTo(clasificacionesAntes);
  }
}
