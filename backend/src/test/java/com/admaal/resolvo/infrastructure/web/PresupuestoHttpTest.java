package com.admaal.resolvo.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
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
@SpringBootTest(
    classes = ResolvoApplication.class,
    properties = {
      "resolvo.ia.presupuesto-diario.llamadas=2",
      "resolvo.ia.presupuesto-diario.tokens=1000000"
    })
@AutoConfigureMockMvc
@Import(IaDeMentira.class)
class PresupuestoHttpTest {

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  private static UUID organizacionDe(String token) {
    String carga = token.split("[.]")[1];
    String json = new String(Base64.getUrlDecoder().decode(carga), StandardCharsets.UTF_8);
    return UUID.fromString(JsonPath.read(json, "$.organizacionId"));
  }

  private void gastarLlamadas(UUID organizacion, int cuantas) {
    for (int i = 0; i < cuantas; i++) {
      jdbc.update(
          """
          insert into llamada_ia
            (id, organizacion_id, operacion, modelo, tokens_entrada, tokens_salida, milisegundos, exito, ocurrida_en)
          values (?, ?, 'sugerir', 'm', 10, 5, 100, true, ?)
          """,
          UUID.randomUUID(),
          organizacion,
          Timestamp.from(Instant.now()));
    }
  }

  @Test
  void conElPresupuestoDelDiaAgotadoSugerirDa429SinLlamarAlModelo() throws Exception {
    HttpAyuda.Organizacion org = HttpAyuda.organizacionNueva(mvc, jdbc);
    String ticket = HttpAyuda.crearTicket(mvc, org.cliente(), org.categoriaId(), "No exporta");
    UUID organizacion = organizacionDe(org.agente());
    gastarLlamadas(organizacion, 2);

    mvc.perform(
            post("/api/tickets/%s/sugerencias".formatted(ticket))
                .header("Authorization", "Bearer " + org.agente()))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.error").value("ia-presupuesto-agotado"));

    Integer filas =
        jdbc.queryForObject(
            "select count(*) from llamada_ia where organizacion_id = ?", Integer.class, organizacion);
    assertThat(filas).isEqualTo(2);
  }

  @Test
  void tambienLimitaLasClasificaciones() throws Exception {
    HttpAyuda.Organizacion org = HttpAyuda.organizacionNueva(mvc, jdbc);
    String ticket = HttpAyuda.crearTicket(mvc, org.cliente(), org.categoriaId(), "No exporta");
    gastarLlamadas(organizacionDe(org.agente()), 2);

    mvc.perform(
            post("/api/tickets/%s/clasificaciones".formatted(ticket))
                .header("Authorization", "Bearer " + org.agente()))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.error").value("ia-presupuesto-agotado"));
  }

  @Test
  void elGastoDeUnaOrganizacionNoBloqueaALaOtra() throws Exception {
    HttpAyuda.Organizacion gastadora = HttpAyuda.organizacionNueva(mvc, jdbc);
    HttpAyuda.Organizacion tranquila = HttpAyuda.organizacionNueva(mvc, jdbc);
    String ticket =
        HttpAyuda.crearTicket(mvc, tranquila.cliente(), tranquila.categoriaId(), "No exporta");
    gastarLlamadas(organizacionDe(gastadora.agente()), 5);

    // Sin fuentes en la organizacion nueva: el caso de uso llega a ejecutarse y responde 422.
    mvc.perform(
            post("/api/tickets/%s/sugerencias".formatted(ticket))
                .header("Authorization", "Bearer " + tranquila.agente()))
        .andExpect(status().isUnprocessableEntity());
  }
}
