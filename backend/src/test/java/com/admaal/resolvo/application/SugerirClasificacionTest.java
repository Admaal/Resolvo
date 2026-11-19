package com.admaal.resolvo.application;

import com.admaal.resolvo.application.soporte.PresupuestoFalso;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.Banco;
import com.admaal.resolvo.application.soporte.ClasificacionesEnMemoria;
import com.admaal.resolvo.application.soporte.ClasificadorFalso;
import com.admaal.resolvo.application.soporte.RelojManual;
import com.admaal.resolvo.application.soporte.RepositorioEnMemoria;
import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.Clasificacion;
import com.admaal.resolvo.domain.EstadoClasificacion;
import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TransicionNoPermitida;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SugerirClasificacionTest {

  private static final Instant AHORA = Instant.parse("2026-11-16T10:00:00Z");

  private final OrganizacionId acme = new OrganizacionId(UUID.randomUUID());
  private final OrganizacionId otra = new OrganizacionId(UUID.randomUUID());
  private final UsuarioId agente = new UsuarioId(UUID.randomUUID());
  private final Categoria incidencia = new Categoria(new CategoriaId(UUID.randomUUID()), acme, "Incidencia");
  private final Categoria consulta = new Categoria(new CategoriaId(UUID.randomUUID()), acme, "Consulta");
  private final RepositorioEnMemoria tickets = new RepositorioEnMemoria();
  private final ClasificacionesEnMemoria clasificaciones = new ClasificacionesEnMemoria();
  private final ClasificadorFalso clasificador = new ClasificadorFalso();
  private final Ticket ticket =
      Banco.abrir(tickets, acme, new UsuarioId(UUID.randomUUID()), Prioridad.BAJA, AHORA);

  private final SugerirClasificacion caso =
      new SugerirClasificacion(
          tickets,
          clasificaciones,
          organizacion -> organizacion.equals(acme) ? List.of(incidencia, consulta) : List.of(),
          clasificador,
          new RelojManual(AHORA));

  private Clasificacion sugerir() {
    return caso.ejecutar(acme, ticket.id(), agente, Rol.AGENTE);
  }

  @Test
  void proponeLaCategoriaYLaPrioridadDelClasificador() {
    clasificador.respondiendo("Consulta", Prioridad.URGENTE, "Pide una factura");

    Clasificacion clasificacion = sugerir();

    assertThat(clasificacion.categoriaId()).isEqualTo(consulta.id());
    assertThat(clasificacion.prioridad()).isEqualTo(Prioridad.URGENTE);
    assertThat(clasificacion.motivo()).isEqualTo("Pide una factura");
    assertThat(clasificacion.modelo()).isEqualTo("clasificador-falso");
    assertThat(clasificacion.estado()).isEqualTo(EstadoClasificacion.PENDIENTE);
    assertThat(clasificacion.creadaEn()).isEqualTo(AHORA);
    assertThat(clasificaciones.pendienteDe(acme, ticket.id())).contains(clasificacion);
  }

  @Test
  void alClasificadorSoloLeLlegaLasCategoriasDeLaOrganizacion() {
    sugerir();

    assertThat(clasificador.recibidas()).containsExactly(incidencia, consulta);
  }

  @Test
  void reutilizaLaPendienteSinLlamarAlModelo() {
    Clasificacion primera = sugerir();

    Clasificacion segunda = sugerir();

    assertThat(segunda).isSameAs(primera);
    assertThat(clasificador.llamadas()).isEqualTo(1);
    assertThat(clasificaciones.total()).isEqualTo(1);
  }

  @Test
  void laCategoriaSeEncuentraSinImportarMayusculas() {
    clasificador.respondiendo("  incidencia ", Prioridad.ALTA, "motivo");

    assertThat(sugerir().categoriaId()).isEqualTo(incidencia.id());
  }

  @Test
  void categoriaInventadaEsIaNoDisponible() {
    clasificador.respondiendo("Facturacion", Prioridad.ALTA, "motivo");

    assertThatThrownBy(this::sugerir)
        .isInstanceOf(IaNoDisponible.class)
        .hasMessage("clasificacion-invalida");
    assertThat(clasificaciones.total()).isZero();
  }

  @Test
  void motivoVacioEsIaNoDisponible() {
    clasificador.respondiendo("Consulta", Prioridad.ALTA, " ");

    assertThatThrownBy(this::sugerir)
        .isInstanceOf(IaNoDisponible.class)
        .hasMessage("clasificacion-invalida");
    assertThat(clasificaciones.total()).isZero();
  }

  @Test
  void prioridadAusenteEsIaNoDisponible() {
    clasificador.respondiendo("Consulta", null, "motivo");

    assertThatThrownBy(this::sugerir)
        .isInstanceOf(IaNoDisponible.class)
        .hasMessage("clasificacion-invalida");
  }

  @Test
  void categoriaAusenteEsIaNoDisponible() {
    clasificador.respondiendo(null, Prioridad.ALTA, "motivo");

    assertThatThrownBy(this::sugerir)
        .isInstanceOf(IaNoDisponible.class)
        .hasMessage("clasificacion-invalida");
  }

  @Test
  void siElProveedorFallaNoSeGuardaNada() {
    clasificador.fallando(new IaNoDisponible("ia-no-disponible"));

    assertThatThrownBy(this::sugerir).isInstanceOf(IaNoDisponible.class);
    assertThat(clasificaciones.total()).isZero();
  }

  @Test
  void clienteRecibeProhibidoSinLlamarAlModelo() {
    assertThatThrownBy(() -> caso.ejecutar(acme, ticket.id(), agente, Rol.CLIENTE))
        .isInstanceOf(Prohibido.class);
    assertThat(clasificador.llamadas()).isZero();
  }

  @Test
  void ticketDeOtraOrganizacionEsNoEncontrado() {
    assertThatThrownBy(() -> caso.ejecutar(otra, ticket.id(), agente, Rol.AGENTE))
        .isInstanceOf(NoEncontrado.class);
    assertThat(clasificador.llamadas()).isZero();
  }

  @Test
  void unTicketResueltoNoSeClasificaNiGastaCuota() {
    ticket.transicionar(EstadoTicket.RESUELTO, Rol.AGENTE);

    assertThatThrownBy(this::sugerir).isInstanceOf(TransicionNoPermitida.class);
    assertThat(clasificador.llamadas()).isZero();
  }

  // --- Presupuesto diario ---------------------------------------------------------------------

  private SugerirClasificacion casoConPresupuesto(PresupuestoFalso presupuesto) {
    return new SugerirClasificacion(
        tickets,
        clasificaciones,
        organizacion -> organizacion.equals(acme) ? List.of(incidencia, consulta) : List.of(),
        clasificador,
        new RelojManual(AHORA),
        presupuesto);
  }

  @Test
  void conElPresupuestoAgotadoNoLlamaAlModelo() {
    PresupuestoFalso presupuesto = new PresupuestoFalso();
    presupuesto.agotar();

    assertThatThrownBy(
            () -> casoConPresupuesto(presupuesto).ejecutar(acme, ticket.id(), agente, Rol.AGENTE))
        .isInstanceOf(PresupuestoAgotado.class);

    assertThat(clasificador.llamadas()).isZero();
    assertThat(clasificaciones.total()).isZero();
  }

  @Test
  void unaPendienteSeDevuelveAunqueElPresupuestoEsteAgotado() {
    PresupuestoFalso presupuesto = new PresupuestoFalso();
    Clasificacion primera =
        casoConPresupuesto(presupuesto).ejecutar(acme, ticket.id(), agente, Rol.AGENTE);
    presupuesto.agotar();

    Clasificacion segunda =
        casoConPresupuesto(presupuesto).ejecutar(acme, ticket.id(), agente, Rol.AGENTE);

    assertThat(segunda).isSameAs(primera);
    assertThat(presupuesto.reservas()).isEqualTo(1);
  }

  @Test
  void laReservaSeLiberaAlTerminar() {
    PresupuestoFalso presupuesto = new PresupuestoFalso();

    casoConPresupuesto(presupuesto).ejecutar(acme, ticket.id(), agente, Rol.AGENTE);

    assertThat(presupuesto.reservas()).isEqualTo(1);
    assertThat(presupuesto.abiertas()).isZero();
  }

  @Test
  void laReservaSeLiberaTambienSiLaLlamadaFalla() {
    clasificador.respondiendo("Facturacion", Prioridad.ALTA, "motivo");
    PresupuestoFalso presupuesto = new PresupuestoFalso();

    assertThatThrownBy(
            () -> casoConPresupuesto(presupuesto).ejecutar(acme, ticket.id(), agente, Rol.AGENTE))
        .isInstanceOf(IaNoDisponible.class);

    assertThat(presupuesto.reservas()).isEqualTo(1);
    assertThat(presupuesto.abiertas()).isZero();
  }

  @Test
  void unClienteNoGastaPresupuesto() {
    PresupuestoFalso presupuesto = new PresupuestoFalso();

    assertThatThrownBy(
            () -> casoConPresupuesto(presupuesto).ejecutar(acme, ticket.id(), agente, Rol.CLIENTE))
        .isInstanceOf(Prohibido.class);

    assertThat(presupuesto.reservas()).isZero();
  }
}
