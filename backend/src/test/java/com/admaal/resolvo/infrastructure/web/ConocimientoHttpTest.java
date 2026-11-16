package com.admaal.resolvo.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.admaal.resolvo.application.soporte.EmbebedorFalso;
import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
@AutoConfigureMockMvc
@Import(IaDeMentira.class)
class ConocimientoHttpTest {

  private static final String ACME = "11111111-1111-1111-1111-111111111111";

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired EmbebedorFalso embebedor;

  @BeforeEach
  void limpiar() {
    jdbc.update("delete from cita");
    jdbc.update("delete from sugerencia");
    jdbc.update("delete from fragmento");
    embebedor.reiniciar();
  }

  private int fragmentos() {
    return jdbc.queryForObject(
        "select count(*) from fragmento where organizacion_id = ?::uuid", Integer.class, ACME);
  }

  @Test
  void reindexarSoloAdmin() throws Exception {
    String agente = HttpAyuda.token(mvc, "agente@acme.test");
    String cliente = HttpAyuda.token(mvc, "cliente@acme.test");
    String admin = HttpAyuda.token(mvc, "admin@acme.test");

    mvc.perform(post("/api/conocimiento/reindexar").header("Authorization", "Bearer " + cliente))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/conocimiento/reindexar").header("Authorization", "Bearer " + agente))
        .andExpect(status().isForbidden());
    assertThat(fragmentos()).isZero();

    mvc.perform(post("/api/conocimiento/reindexar").header("Authorization", "Bearer " + admin))
        .andExpect(status().isNoContent());
    assertThat(fragmentos()).isGreaterThanOrEqualTo(8);
  }

  @Test
  void reindexarDosVecesNoDuplicaNiGastaCuota() throws Exception {
    String admin = HttpAyuda.token(mvc, "admin@acme.test");
    mvc.perform(post("/api/conocimiento/reindexar").header("Authorization", "Bearer " + admin))
        .andExpect(status().isNoContent());
    int fragmentos = fragmentos();
    int llamadas = embebedor.llamadas();

    mvc.perform(post("/api/conocimiento/reindexar").header("Authorization", "Bearer " + admin))
        .andExpect(status().isNoContent());

    assertThat(fragmentos()).isEqualTo(fragmentos);
    assertThat(embebedor.llamadas()).isEqualTo(llamadas);
  }

  @Test
  void reindexarSoloTocaLaOrganizacionDelAdmin() throws Exception {
    var ajena = HttpAyuda.organizacionNueva(mvc, jdbc);
    String admin = HttpAyuda.token(mvc, "admin@acme.test");

    mvc.perform(post("/api/conocimiento/reindexar").header("Authorization", "Bearer " + admin))
        .andExpect(status().isNoContent());

    assertThat(
            jdbc.queryForObject(
                "select count(*) from fragmento where organizacion_id <> ?::uuid", Integer.class, ACME))
        .isZero();
    assertThat(ajena.agente()).isNotBlank();
  }
}
