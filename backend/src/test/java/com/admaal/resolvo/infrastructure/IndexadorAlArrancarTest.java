package com.admaal.resolvo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.web.IaDeMentira;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Al arrancar con IA disponible, los articulos de la semilla quedan indexados sin intervencion. */
@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
@Import(IaDeMentira.class)
class IndexadorAlArrancarTest {

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
    registro.add("resolvo.ia.indexar-al-arrancar", () -> "true");
  }

  @Autowired JdbcTemplate jdbc;

  @Test
  void losArticulosDeLaSemillaQuedanIndexadosAlArrancar() {
    Integer indexados =
        jdbc.queryForObject(
            "select count(distinct fuente_id) from fragmento where fuente = 'ARTICULO'", Integer.class);

    assertThat(indexados).isEqualTo(8);
  }
}
