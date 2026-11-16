package com.admaal.resolvo.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Utilidades compartidas por los tests HTTP de la fase 3. */
final class HttpAyuda {

  static final String INCIDENCIA = "33333333-3333-3333-3333-333333333331";

  record Organizacion(String agente, String cliente, String categoriaId) {}

  private HttpAyuda() {}

  static String token(MockMvc mvc, String email) throws Exception {
    MvcResult resultado =
        mvc.perform(
                post("/api/sesion")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"%s\",\"contrasena\":\"demo-resolvo\"}".formatted(email)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(resultado.getResponse().getContentAsString(), "$.access");
  }

  /** Crea una organizacion sin articulos ni fragmentos, con un agente y un cliente. */
  static Organizacion organizacionNueva(MockMvc mvc, JdbcTemplate jdbc) throws Exception {
    UUID org = UUID.randomUUID();
    UUID categoria = UUID.randomUUID();
    String hash =
        jdbc.queryForObject(
            "select hash_contrasena from usuario where email = ?",
            String.class,
            "cliente@acme.test");
    jdbc.update("insert into organizacion (id, nombre) values (?, 'Nueva')", org);
    jdbc.update(
        "insert into categoria (id, organizacion_id, nombre) values (?, ?, 'Incidencia')",
        categoria,
        org);
    jdbc.update("insert into secuencia_ticket (organizacion_id, ultimo) values (?, 0)", org);
    String agente = "agente-" + org + "@nueva.test";
    String cliente = "cliente-" + org + "@nueva.test";
    for (String[] fila : new String[][] {{agente, "AGENTE"}, {cliente, "CLIENTE"}}) {
      jdbc.update(
          """
          insert into usuario (id, organizacion_id, email, rol, hash_contrasena, fallos_seguidos)
          values (?, ?, ?, ?, ?, 0)
          """,
          UUID.randomUUID(),
          org,
          fila[0],
          fila[1],
          hash);
    }
    return new Organizacion(token(mvc, agente), token(mvc, cliente), categoria.toString());
  }

  static String crearTicket(MockMvc mvc, String token, String categoriaId, String asunto)
      throws Exception {
    MvcResult resultado =
        mvc.perform(
                post("/api/tickets")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"categoriaId":"%s","prioridad":"ALTA","asunto":"%s","cuerpo":"No puedo exportar el informe a CSV"}
                        """
                            .formatted(categoriaId, asunto)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
  }
}
