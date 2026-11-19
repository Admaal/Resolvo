package com.admaal.resolvo.infrastructure.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.domain.LlamadaIa;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
class RegistroLlamadasJdbcTest {

  private static final OrganizacionId ACME =
      new OrganizacionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  private static final Instant T0 = Instant.parse("2026-11-19T10:00:00Z");

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired RegistroLlamadasJdbc registro;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void limpiar() {
    jdbc.update("delete from llamada_ia");
  }

  private OrganizacionId otraOrganizacion() {
    UUID id = UUID.randomUUID();
    jdbc.update("insert into organizacion (id, nombre) values (?, 'Otra')", id);
    return new OrganizacionId(id);
  }

  private static LlamadaIa llamada(
      OrganizacionId org, String operacion, int entrada, boolean exito, Instant cuando) {
    return new LlamadaIa(UUID.randomUUID(), org, operacion, "m1", entrada, 3, 4_321, exito, cuando);
  }

  @Test
  void guardaYRecuperaTodosLosCamposDeUnaLlamada() {
    LlamadaIa exito = llamada(ACME, "sugerir", 120, true, T0);
    LlamadaIa fallo = llamada(ACME, "clasificar", 0, false, T0.plusSeconds(1));

    registro.registrar(exito);
    registro.registrar(fallo);

    assertThat(registro.deLaOrganizacion(ACME, T0.minusSeconds(1), T0.plusSeconds(10)))
        .containsExactly(exito, fallo);
  }

  @Test
  void soloDevuelveLasDeLaOrganizacionYDentroDelRangoSemiabierto() {
    OrganizacionId otra = otraOrganizacion();
    LlamadaIa antes = llamada(ACME, "sugerir", 1, true, T0.minusSeconds(1));
    LlamadaIa enElInicio = llamada(ACME, "sugerir", 2, true, T0);
    LlamadaIa enElFin = llamada(ACME, "sugerir", 4, true, T0.plusSeconds(60));
    LlamadaIa ajena = llamada(otra, "sugerir", 8, true, T0.plusSeconds(5));
    for (LlamadaIa llamada : new LlamadaIa[] {antes, enElInicio, enElFin, ajena}) {
      registro.registrar(llamada);
    }

    assertThat(registro.deLaOrganizacion(ACME, T0, T0.plusSeconds(60))).containsExactly(enElInicio);
    assertThat(registro.deLaOrganizacion(otra, T0, T0.plusSeconds(60))).containsExactly(ajena);
  }

  @Test
  void unaLlamadaSinOrganizacionSeGuardaPeroNingunaOrganizacionLaVe() {
    registro.registrar(llamada(null, "sin-contexto", 1, true, T0));

    assertThat(jdbc.queryForObject("select count(*) from llamada_ia", Integer.class)).isEqualTo(1);
    assertThat(registro.deLaOrganizacion(ACME, T0.minusSeconds(1), T0.plusSeconds(1))).isEmpty();
  }
}
