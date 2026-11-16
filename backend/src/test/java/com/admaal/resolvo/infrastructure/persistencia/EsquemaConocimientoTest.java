package com.admaal.resolvo.infrastructure.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
class EsquemaConocimientoTest {

  private static final String ACME = "11111111-1111-1111-1111-111111111111";
  private static final String TICKET = "44444444-4444-4444-4444-444444444441";

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource datos;

  @AfterEach
  void limpiar() {
    jdbc.update("delete from cita");
    jdbc.update("delete from sugerencia");
    jdbc.update("delete from fragmento");
  }

  @Test
  void laSemillaDejaOchoArticulosEnAcme() {
    Integer articulos =
        jdbc.queryForObject(
            "select count(*) from articulo where organizacion_id = ?::uuid", Integer.class, ACME);

    assertThat(articulos).isEqualTo(8);
  }

  @Test
  void reaplicarLaSemillaDeArticulosNoDuplicaNada() {
    new ResourceDatabasePopulator(new ClassPathResource("db/migration/V5__semilla_articulos.sql"))
        .execute(datos);

    assertThat(jdbc.queryForObject("select count(*) from articulo", Integer.class)).isEqualTo(8);
  }

  @Test
  void unEmbeddingDeOtraDimensionSeRechaza() {
    assertThatThrownBy(() -> insertarFragmento("[1,2,3]", 0)).isInstanceOf(DataAccessException.class);
  }

  @Test
  void elMismoFragmentoNoSeIndexaDosVecesParaUnModelo() {
    String vector = "[" + "0,".repeat(767) + "0]";
    insertarFragmento(vector, 0);

    assertThatThrownBy(() -> insertarFragmento(vector, 0)).isInstanceOf(DataAccessException.class);
  }

  @Test
  void soloPuedeHaberUnaSugerenciaPendientePorTicket() {
    insertarSugerencia("77777777-7777-7777-7777-777777777771", "PENDIENTE");

    assertThatThrownBy(() -> insertarSugerencia("77777777-7777-7777-7777-777777777772", "PENDIENTE"))
        .isInstanceOf(DataAccessException.class);

    jdbc.update(
        "update sugerencia set estado = 'DESCARTADA' where id = '77777777-7777-7777-7777-777777777771'::uuid");
    insertarSugerencia("77777777-7777-7777-7777-777777777773", "PENDIENTE");
  }

  private void insertarFragmento(String vector, int orden) {
    jdbc.update(
        "insert into fragmento (id, organizacion_id, fuente, fuente_id, hash_fuente, modelo, orden,"
            + " titulo, texto, embedding) values (gen_random_uuid(), ?::uuid, 'ARTICULO',"
            + " '66666666-6666-6666-6666-666666666661'::uuid, 'h', 'm', ?, 't', 'x', ?::vector)",
        ACME,
        orden,
        vector);
  }

  private void insertarSugerencia(String id, String estado) {
    jdbc.update(
        "insert into sugerencia (id, organizacion_id, ticket_id, borrador, estado, modelo, creada_en)"
            + " values (?::uuid, ?::uuid, ?::uuid, 'b', ?, 'm', now())",
        id,
        ACME,
        TICKET,
        estado);
  }
}
