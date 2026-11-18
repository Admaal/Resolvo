package com.admaal.resolvo.infrastructure.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.Clasificacion;
import com.admaal.resolvo.domain.ClasificacionId;
import com.admaal.resolvo.domain.EstadoClasificacion;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.TransicionNoPermitida;
import com.admaal.resolvo.domain.UsuarioId;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import java.time.Instant;
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
class ClasificacionesJdbcTest {

  private static final OrganizacionId ACME =
      new OrganizacionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  private static final TicketId TICKET =
      new TicketId(UUID.fromString("44444444-4444-4444-4444-444444444441"));
  private static final UsuarioId AGENTE =
      new UsuarioId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
  private static final CategoriaId INCIDENCIA =
      new CategoriaId(UUID.fromString("33333333-3333-3333-3333-333333333331"));
  private static final Instant AHORA = Instant.parse("2026-11-16T10:00:00Z");

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired ClasificacionesJdbc clasificaciones;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void limpiar() {
    jdbc.update("delete from clasificacion");
  }

  private Clasificacion nueva() {
    return Clasificacion.proponer(
        ClasificacionId.nuevo(),
        ACME,
        TICKET,
        INCIDENCIA,
        Prioridad.URGENTE,
        "El servicio esta caido",
        "modelo-x",
        AHORA);
  }

  @Test
  void guardaYRecuperaLaPropuesta() {
    Clasificacion original = nueva();

    clasificaciones.guardar(original);
    Clasificacion leida = clasificaciones.porId(ACME, original.id()).orElseThrow();

    assertThat(leida.id()).isEqualTo(original.id());
    assertThat(leida.ticketId()).isEqualTo(TICKET);
    assertThat(leida.categoriaId()).isEqualTo(INCIDENCIA);
    assertThat(leida.prioridad()).isEqualTo(Prioridad.URGENTE);
    assertThat(leida.motivo()).isEqualTo("El servicio esta caido");
    assertThat(leida.modelo()).isEqualTo("modelo-x");
    assertThat(leida.creadaEn()).isEqualTo(AHORA);
    assertThat(leida.estado()).isEqualTo(EstadoClasificacion.PENDIENTE);
    assertThat(leida.resueltaPor()).isNull();
  }

  @Test
  void porIdYPendienteDeFiltranPorOrganizacion() {
    Clasificacion original = nueva();
    clasificaciones.guardar(original);
    UUID otra = UUID.randomUUID();
    jdbc.update("insert into organizacion (id, nombre) values (?, 'Otra')", otra);

    assertThat(clasificaciones.porId(new OrganizacionId(otra), original.id())).isEmpty();
    assertThat(clasificaciones.pendienteDe(new OrganizacionId(otra), TICKET)).isEmpty();
    assertThat(clasificaciones.pendienteDe(ACME, TICKET)).isPresent();
  }

  @Test
  void pendienteDeSoloDevuelveLasPendientes() {
    Clasificacion original = nueva();
    clasificaciones.guardar(original);

    original.descartar(AGENTE, Rol.AGENTE, AHORA);
    clasificaciones.guardar(original);

    assertThat(clasificaciones.pendienteDe(ACME, TICKET)).isEmpty();
  }

  @Test
  void laResolucionSeGuardaConQuienYCuando() {
    Clasificacion original = nueva();
    clasificaciones.guardar(original);
    original.aplicar(AGENTE, Rol.AGENTE, AHORA.plusSeconds(9));

    clasificaciones.guardar(original);
    Clasificacion leida = clasificaciones.porId(ACME, original.id()).orElseThrow();

    assertThat(leida.estado()).isEqualTo(EstadoClasificacion.APLICADA);
    assertThat(leida.resueltaPor()).isEqualTo(AGENTE);
    assertThat(leida.resueltaEn()).isEqualTo(AHORA.plusSeconds(9));
  }

  @Test
  void unaSegundaResolucionConcurrenteSeRechaza() {
    Clasificacion original = nueva();
    clasificaciones.guardar(original);
    Clasificacion primera = clasificaciones.porId(ACME, original.id()).orElseThrow();
    Clasificacion segunda = clasificaciones.porId(ACME, original.id()).orElseThrow();
    primera.aplicar(AGENTE, Rol.AGENTE, AHORA);
    segunda.descartar(AGENTE, Rol.AGENTE, AHORA);

    clasificaciones.guardar(primera);

    assertThatThrownBy(() -> clasificaciones.guardar(segunda))
        .isInstanceOf(TransicionNoPermitida.class);
    assertThat(clasificaciones.porId(ACME, original.id()).orElseThrow().estado())
        .isEqualTo(EstadoClasificacion.APLICADA);
  }

  @Test
  void dosPendientesDelMismoTicketChocanConElIndice() {
    clasificaciones.guardar(nueva());

    assertThatThrownBy(() -> clasificaciones.guardar(nueva()))
        .isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void guardarUnaPendienteYaGuardadaNoHaceNada() {
    Clasificacion original = nueva();
    clasificaciones.guardar(original);

    clasificaciones.guardar(original);

    assertThat(jdbc.queryForObject("select count(*) from clasificacion", Integer.class)).isEqualTo(1);
  }
}
