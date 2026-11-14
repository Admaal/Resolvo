package com.admaal.resolvo.infrastructure.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
class SemillaDemoTest {

  private static final String ACME = "11111111-1111-1111-1111-111111111111";
  private static final String SOLO_DEMO = "id::text like '44444444-%'";

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource datos;

  @Test
  void laMigracionDejaTicketsDeDemoVariadosEnAcme() {
    Integer total =
        jdbc.queryForObject(
            "select count(*) from ticket where organizacion_id = ?::uuid and " + SOLO_DEMO,
            Integer.class,
            ACME);
    Integer estados =
        jdbc.queryForObject(
            "select count(distinct estado) from ticket where " + SOLO_DEMO, Integer.class);
    Integer prioridades =
        jdbc.queryForObject(
            "select count(distinct prioridad) from ticket where " + SOLO_DEMO, Integer.class);

    assertThat(total).isEqualTo(6);
    assertThat(estados).isGreaterThanOrEqualTo(3);
    assertThat(prioridades).isGreaterThanOrEqualTo(3);
  }

  @Test
  void reaplicarLaSemillaNoDuplicaNada() {
    int ticketsAntes = contar("ticket");
    int comentariosAntes = contar("comentario");

    new ResourceDatabasePopulator(new ClassPathResource("db/migration/V3__semilla_demo.sql"))
        .execute(datos);

    assertThat(contar("ticket")).isEqualTo(ticketsAntes);
    assertThat(contar("comentario")).isEqualTo(comentariosAntes);
  }

  @Test
  void laSecuenciaNoQuedaPorDebajoDeLosCodigosSembrados() {
    Integer ultimo =
        jdbc.queryForObject(
            "select ultimo from secuencia_ticket where organizacion_id = ?::uuid",
            Integer.class,
            ACME);

    assertThat(ultimo).isGreaterThanOrEqualTo(6);
  }

  @Test
  void losTicketsDeDemoTienenVencimientoPosteriorALaApertura() {
    Integer invalidos =
        jdbc.queryForObject(
            "select count(*) from ticket where " + SOLO_DEMO + " and vence_el <= abierto_en",
            Integer.class);

    assertThat(invalidos).isZero();
  }

  private int contar(String tabla) {
    Integer cuenta = jdbc.queryForObject("select count(*) from " + tabla, Integer.class);
    return cuenta == null ? 0 : cuenta;
  }
}
