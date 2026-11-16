package com.admaal.resolvo.infrastructure.web;

import static com.admaal.resolvo.infrastructure.web.HttpAyuda.INCIDENCIA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Sin perfil gemini ni clave: la app funciona y la IA responde 503 con un motivo claro. */
@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
@AutoConfigureMockMvc
class SugerenciaSinIaHttpTest {

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @Test
  void sinClaveSugerirResponde503ConElMotivo() throws Exception {
    String agente = HttpAyuda.token(mvc, "agente@acme.test");
    String ticket = HttpAyuda.crearTicket(mvc, agente, INCIDENCIA, "Sin clave");

    mvc.perform(post("/api/tickets/" + ticket + "/sugerencias").header("Authorization", "Bearer " + agente))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("ia-no-configurada"));

    mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + agente))
        .andExpect(status().isOk());
  }

  @Test
  void sinClaveReindexarResponde503PeroElRestoDeLaAppSigue() throws Exception {
    String admin = HttpAyuda.token(mvc, "admin@acme.test");

    mvc.perform(post("/api/conocimiento/reindexar").header("Authorization", "Bearer " + admin))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("ia-no-configurada"));

    mvc.perform(get("/api/tickets").header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
  }

  @Test
  void sinClaveResolverUnTicketSigueFuncionando() throws Exception {
    String agente = HttpAyuda.token(mvc, "agente@acme.test");
    String ticket = HttpAyuda.crearTicket(mvc, agente, INCIDENCIA, "Se resuelve igual");

    mvc.perform(
            post("/api/tickets/" + ticket + "/transicion")
                .header("Authorization", "Bearer " + agente)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"RESUELTO\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("RESUELTO"));
  }
}
