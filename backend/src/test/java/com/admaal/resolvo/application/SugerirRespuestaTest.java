package com.admaal.resolvo.application;

import com.admaal.resolvo.application.soporte.PresupuestoFalso;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.Banco;
import com.admaal.resolvo.application.soporte.BaseConocimientoEnMemoria;
import com.admaal.resolvo.application.soporte.EmbebedorFalso;
import com.admaal.resolvo.application.soporte.RedactorFalso;
import com.admaal.resolvo.application.soporte.RelojManual;
import com.admaal.resolvo.application.soporte.RepositorioEnMemoria;
import com.admaal.resolvo.application.soporte.SugerenciasEnMemoria;
import com.admaal.resolvo.domain.Cita;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.FragmentoId;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.SinFuentes;
import com.admaal.resolvo.domain.Sugerencia;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SugerirRespuestaTest {

  private static final Instant AHORA = Instant.parse("2026-11-16T10:00:00Z");

  private final OrganizacionId acme = new OrganizacionId(UUID.randomUUID());
  private final OrganizacionId otra = new OrganizacionId(UUID.randomUUID());
  private final UsuarioId agente = new UsuarioId(UUID.randomUUID());
  private final RepositorioEnMemoria tickets = new RepositorioEnMemoria();
  private final SugerenciasEnMemoria sugerencias = new SugerenciasEnMemoria();
  private final EmbebedorFalso embebedor = new EmbebedorFalso();
  private final BaseConocimientoEnMemoria base = new BaseConocimientoEnMemoria();
  private final RedactorFalso redactor = new RedactorFalso();
  private final Ticket ticket =
      Banco.abrir(tickets, acme, new UsuarioId(UUID.randomUUID()), Prioridad.ALTA, AHORA);

  private SugerirRespuesta caso(RedactorFalso redactorEnUso) {
    return new SugerirRespuesta(
        tickets, sugerencias, embebedor, base, redactorEnUso, new RelojManual(AHORA));
  }

  private SugerirRespuesta casoConUmbral(double distanciaMaxima) {
    return new SugerirRespuesta(
        tickets, sugerencias, embebedor, base, redactor, new RelojManual(AHORA), distanciaMaxima);
  }

  private Sugerencia sugerir() {
    return caso(redactor).ejecutar(acme, ticket.id(), agente, Rol.AGENTE);
  }

  @Test
  void sugiereConLasCitasDeLasFuentes() {
    Fragmento fuente = base.sembrar(acme, UUID.randomUUID(), "Reiniciar router", "Pulsa reset.");

    Sugerencia sugerencia = sugerir();

    assertThat(sugerencia.borrador()).isEqualTo("Borrador sugerido");
    assertThat(sugerencia.modelo()).isEqualTo("redactor-falso");
    assertThat(sugerencia.citas()).extracting(Cita::fragmentoId).containsExactly(fuente.id());
    assertThat(sugerencia.ticketId()).isEqualTo(ticket.id());
    assertThat(sugerencia.organizacionId()).isEqualTo(acme);
    assertThat(sugerencia.creadaEn()).isEqualTo(AHORA);
    assertThat(sugerencias.total()).isEqualTo(1);
  }

  @Test
  void usaAsuntoYCuerpoDelTicketComoConsulta() {
    base.sembrar(acme, UUID.randomUUID(), "A", "texto");

    sugerir();

    assertThat(embebedor.textos()).containsExactly(ticket.asunto() + "\n" + ticket.cuerpo());
  }

  @Test
  void dobleClicNoDuplicaNiLlamaDosVeces() {
    base.sembrar(acme, UUID.randomUUID(), "A", "texto");

    Sugerencia primera = sugerir();
    Sugerencia segunda = sugerir();

    assertThat(segunda.id()).isEqualTo(primera.id());
    assertThat(redactor.llamadas()).isEqualTo(1);
    assertThat(embebedor.llamadas()).isEqualTo(1);
    assertThat(sugerencias.vecesGuardado()).isEqualTo(1);
  }

  @Test
  void soloRecuperaFragmentosDeSuOrganizacion() {
    Fragmento propio = base.sembrar(acme, UUID.randomUUID(), "Propio", "texto");
    base.sembrar(otra, UUID.randomUUID(), "Ajeno", "secreto de otra org");

    sugerir();

    assertThat(redactor.recibidos()).containsExactly(propio);
  }

  @Test
  void noSeCitaASiMismo() {
    base.sembrar(acme, ticket.id().valor(), "El mismo ticket", "texto");
    Fragmento otro = base.sembrar(acme, UUID.randomUUID(), "Otro", "texto");

    sugerir();

    assertThat(redactor.recibidos()).containsExactly(otro);
  }

  @Test
  void sinFuentesNoLlamaAlRedactor() {
    assertThatThrownBy(this::sugerir).isInstanceOf(SinFuentes.class);

    assertThat(redactor.llamadas()).isZero();
    assertThat(sugerencias.total()).isZero();
  }

  @Test
  void clienteRecibe403SinLlamarAlProveedor() {
    base.sembrar(acme, UUID.randomUUID(), "A", "texto");

    assertThatThrownBy(() -> caso(redactor).ejecutar(acme, ticket.id(), agente, Rol.CLIENTE))
        .isInstanceOf(Prohibido.class);

    assertThat(embebedor.llamadas()).isZero();
    assertThat(redactor.llamadas()).isZero();
  }

  @Test
  void ticketDeOtraOrgEs404() {
    base.sembrar(otra, UUID.randomUUID(), "A", "texto");

    assertThatThrownBy(() -> caso(redactor).ejecutar(otra, ticket.id(), agente, Rol.AGENTE))
        .isInstanceOf(NoEncontrado.class);

    assertThat(redactor.llamadas()).isZero();
  }

  @Test
  void citaInventadaPorElRedactorSeDescarta() {
    Fragmento real = base.sembrar(acme, UUID.randomUUID(), "A", "texto");
    RedactorFalso inventor = new RedactorFalso().citando(List.of(FragmentoId.nuevo(), real.id()));

    Sugerencia sugerencia = caso(inventor).ejecutar(acme, ticket.id(), agente, Rol.AGENTE);

    assertThat(sugerencia.citas()).extracting(Cita::fragmentoId).containsExactly(real.id());
  }

  @Test
  void errorDelRedactorNoDejaSugerencia() {
    base.sembrar(acme, UUID.randomUUID(), "A", "texto");
    RedactorFalso roto = new RedactorFalso().fallando(new IllegalStateException("429"));

    assertThatThrownBy(() -> caso(roto).ejecutar(acme, ticket.id(), agente, Rol.AGENTE))
        .isInstanceOf(IllegalStateException.class);

    assertThat(sugerencias.total()).isZero();
  }

  @Test
  void pasaLaDistanciaMaximaConfigurada() {
    base.sembrar(acme, UUID.randomUUID(), "A", "texto");

    casoConUmbral(0.35).ejecutar(acme, ticket.id(), agente, Rol.AGENTE);

    assertThat(base.ultimaDistanciaMaxima()).isEqualTo(0.35);
  }

  @Test
  void sinUmbralConfiguradoNoFiltraNada() {
    base.sembrar(acme, UUID.randomUUID(), "Lejano", "texto", 1.9);

    sugerir();

    assertThat(base.ultimaDistanciaMaxima()).isEqualTo(BaseConocimiento.SIN_LIMITE);
    assertThat(redactor.llamadas()).isEqualTo(1);
  }

  @Test
  void sinFragmentosBajoElUmbralEsSinFuentesYNoLlamaAlRedactor() {
    base.sembrar(acme, UUID.randomUUID(), "Irrelevante", "texto", 0.8);

    assertThatThrownBy(() -> casoConUmbral(0.5).ejecutar(acme, ticket.id(), agente, Rol.AGENTE))
        .isInstanceOf(SinFuentes.class);

    assertThat(redactor.llamadas()).isZero();
    assertThat(sugerencias.total()).isZero();
  }

  @Test
  void soloLlegaAlRedactorLoQueEstaBajoElUmbral() {
    Fragmento cercano = base.sembrar(acme, UUID.randomUUID(), "Cercano", "texto", 0.2);
    base.sembrar(acme, UUID.randomUUID(), "Lejano", "texto", 0.9);

    casoConUmbral(0.5).ejecutar(acme, ticket.id(), agente, Rol.AGENTE);

    assertThat(redactor.recibidos()).containsExactly(cercano);
  }

  // --- Presupuesto diario ---------------------------------------------------------------------

  private SugerirRespuesta casoConPresupuesto(PresupuestoFalso presupuesto) {
    return new SugerirRespuesta(
        tickets,
        sugerencias,
        embebedor,
        base,
        redactor,
        new RelojManual(AHORA),
        BaseConocimiento.SIN_LIMITE,
        presupuesto);
  }

  @Test
  void conElPresupuestoAgotadoNoLlamaAlModelo() {
    base.sembrar(acme, UUID.randomUUID(), "A", "texto");
    PresupuestoFalso presupuesto = new PresupuestoFalso();
    presupuesto.agotar();

    assertThatThrownBy(
            () -> casoConPresupuesto(presupuesto).ejecutar(acme, ticket.id(), agente, Rol.AGENTE))
        .isInstanceOf(PresupuestoAgotado.class);

    assertThat(embebedor.llamadas()).isZero();
    assertThat(redactor.llamadas()).isZero();
    assertThat(sugerencias.total()).isZero();
  }

  @Test
  void unaPendienteSeDevuelveAunqueElPresupuestoEsteAgotado() {
    base.sembrar(acme, UUID.randomUUID(), "A", "texto");
    PresupuestoFalso presupuesto = new PresupuestoFalso();
    Sugerencia primera =
        casoConPresupuesto(presupuesto).ejecutar(acme, ticket.id(), agente, Rol.AGENTE);
    presupuesto.agotar();

    Sugerencia segunda =
        casoConPresupuesto(presupuesto).ejecutar(acme, ticket.id(), agente, Rol.AGENTE);

    assertThat(segunda.id()).isEqualTo(primera.id());
    assertThat(presupuesto.reservas()).isEqualTo(1);
  }

  @Test
  void laReservaSeLiberaAlTerminar() {
    base.sembrar(acme, UUID.randomUUID(), "A", "texto");
    PresupuestoFalso presupuesto = new PresupuestoFalso();

    casoConPresupuesto(presupuesto).ejecutar(acme, ticket.id(), agente, Rol.AGENTE);

    assertThat(presupuesto.reservas()).isEqualTo(1);
    assertThat(presupuesto.abiertas()).isZero();
  }

  @Test
  void laReservaSeLiberaTambienSiLaLlamadaFalla() {
    PresupuestoFalso presupuesto = new PresupuestoFalso();

    assertThatThrownBy(
            () -> casoConPresupuesto(presupuesto).ejecutar(acme, ticket.id(), agente, Rol.AGENTE))
        .isInstanceOf(SinFuentes.class);

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
