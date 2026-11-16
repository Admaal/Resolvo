package com.admaal.resolvo.infrastructure.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.FragmentoIndexado;
import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.domain.Cita;
import com.admaal.resolvo.domain.Embedding;
import com.admaal.resolvo.domain.EstadoSugerencia;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.FragmentoId;
import com.admaal.resolvo.domain.FuenteConocimiento;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Sugerencia;
import com.admaal.resolvo.domain.SugerenciaId;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.TransicionNoPermitida;
import com.admaal.resolvo.domain.UsuarioId;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
class SugerenciasJdbcTest {

  private static final OrganizacionId ACME =
      new OrganizacionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  private static final TicketId TICKET =
      new TicketId(UUID.fromString("44444444-4444-4444-4444-444444444441"));
  private static final UsuarioId AGENTE =
      new UsuarioId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
  private static final Instant AHORA = Instant.parse("2026-11-16T10:00:00Z");

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired SugerenciasJdbc sugerencias;
  @Autowired BaseConocimientoPgvector base;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void limpiar() {
    jdbc.update("delete from cita");
    jdbc.update("delete from sugerencia");
    jdbc.update("delete from fragmento");
  }

  private Fragmento fragmento(String titulo) {
    UUID fuente = UUID.randomUUID();
    Fragmento fragmento =
        new Fragmento(
            FragmentoId.nuevo(), ACME, FuenteConocimiento.ARTICULO, fuente, titulo, "texto");
    base.reemplazar(
        FuenteConocimiento.ARTICULO,
        fuente,
        "h",
        "m",
        List.of(new FragmentoIndexado(fragmento, new Embedding(new float[Embedding.DIMENSION]))));
    return fragmento;
  }

  private Sugerencia nueva(List<Fragmento> citados) {
    List<FragmentoId> ids = citados.stream().map(Fragmento::id).toList();
    return Sugerencia.crear(
        SugerenciaId.nuevo(), ACME, TICKET, "Borrador", "modelo-x", AHORA, ids, Set.copyOf(ids));
  }

  @Test
  void guardaYRecuperaConSusCitasEnOrden() {
    Fragmento uno = fragmento("Uno");
    Fragmento dos = fragmento("Dos");
    Sugerencia original = nueva(List.of(uno, dos));

    sugerencias.guardar(original);
    Sugerencia leida = sugerencias.porId(ACME, original.id()).orElseThrow();

    assertThat(leida.id()).isEqualTo(original.id());
    assertThat(leida.ticketId()).isEqualTo(TICKET);
    assertThat(leida.borrador()).isEqualTo("Borrador");
    assertThat(leida.modelo()).isEqualTo("modelo-x");
    assertThat(leida.creadaEn()).isEqualTo(AHORA);
    assertThat(leida.estado()).isEqualTo(EstadoSugerencia.PENDIENTE);
    assertThat(leida.citas())
        .containsExactly(new Cita(uno.id(), 1), new Cita(dos.id(), 2));
  }

  @Test
  void laCitaConservaElTituloAunqueSeReindexeLaFuente() {
    Fragmento uno = fragmento("Titulo original");
    Sugerencia original = nueva(List.of(uno));
    sugerencias.guardar(original);

    base.reemplazar(FuenteConocimiento.ARTICULO, uno.fuenteId(), "h2", "m", List.of());

    assertThat(sugerencias.citasDe(original.id()))
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.titulo()).isEqualTo("Titulo original");
              assertThat(c.fuente()).isEqualTo(FuenteConocimiento.ARTICULO);
              assertThat(c.fuenteId()).isEqualTo(uno.fuenteId());
              assertThat(c.orden()).isEqualTo(1);
            });
  }

  @Test
  void unaCitaAUnFragmentoYaReemplazadoSeOmite() {
    Fragmento desaparecido = fragmento("Se va");
    Fragmento queda = fragmento("Queda");
    Sugerencia original = nueva(List.of(desaparecido, queda));
    base.reemplazar(FuenteConocimiento.ARTICULO, desaparecido.fuenteId(), "h2", "m", List.of());

    sugerencias.guardar(original);

    assertThat(sugerencias.citasDe(original.id())).extracting(CitaDetalle::titulo).containsExactly("Queda");
  }

  @Test
  void porIdFiltraPorOrganizacion() {
    Sugerencia original = nueva(List.of());
    sugerencias.guardar(original);
    UUID otra = UUID.randomUUID();
    jdbc.update("insert into organizacion (id, nombre) values (?, 'Otra')", otra);

    assertThat(sugerencias.porId(new OrganizacionId(otra), original.id())).isEmpty();
    assertThat(sugerencias.pendienteDe(new OrganizacionId(otra), TICKET)).isEmpty();
  }

  @Test
  void pendienteDeSoloDevuelveLasPendientes() {
    Sugerencia original = nueva(List.of());
    sugerencias.guardar(original);
    assertThat(sugerencias.pendienteDe(ACME, TICKET)).get().extracting(Sugerencia::id).isEqualTo(original.id());

    original.descartar(AGENTE, Rol.AGENTE, AHORA);
    sugerencias.guardar(original);

    assertThat(sugerencias.pendienteDe(ACME, TICKET)).isEmpty();
  }

  @Test
  void laResolucionSeGuardaConQuienYCuando() {
    Sugerencia original = nueva(List.of());
    sugerencias.guardar(original);
    original.aprobar(AGENTE, Rol.AGENTE, "Texto final", AHORA.plusSeconds(9));

    sugerencias.guardar(original);
    Sugerencia leida = sugerencias.porId(ACME, original.id()).orElseThrow();

    assertThat(leida.estado()).isEqualTo(EstadoSugerencia.APROBADA);
    assertThat(leida.resueltaPor()).isEqualTo(AGENTE);
    assertThat(leida.resueltaEn()).isEqualTo(AHORA.plusSeconds(9));
  }

  @Test
  void unaSegundaResolucionConcurrenteSeRechaza() {
    Sugerencia original = nueva(List.of());
    sugerencias.guardar(original);
    Sugerencia primera = sugerencias.porId(ACME, original.id()).orElseThrow();
    Sugerencia segunda = sugerencias.porId(ACME, original.id()).orElseThrow();
    primera.aprobar(AGENTE, Rol.AGENTE, "uno", AHORA);
    segunda.descartar(AGENTE, Rol.AGENTE, AHORA);

    sugerencias.guardar(primera);

    assertThatThrownBy(() -> sugerencias.guardar(segunda)).isInstanceOf(TransicionNoPermitida.class);
    assertThat(sugerencias.porId(ACME, original.id()).orElseThrow().estado())
        .isEqualTo(EstadoSugerencia.APROBADA);
  }

  @Test
  void soloPuedeHaberUnaPendientePorTicket() {
    sugerencias.guardar(nueva(List.of()));

    assertThatThrownBy(() -> sugerencias.guardar(nueva(List.of())))
        .isInstanceOf(DuplicateKeyException.class);
  }
}
