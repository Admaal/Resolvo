package com.admaal.resolvo.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
@AutoConfigureMockMvc
class TicketHttpTest {

  private static final String INCIDENCIA = "33333333-3333-3333-3333-333333333331";

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @Test
  void otraOrganizacionResponde404() throws Exception {
    String cliente = token("cliente@acme.test");
    String ticket = crear(cliente, "ALTA", "Cae el acceso");
    String beta = tokenBeta();

    MvcResult ajena = ver(beta, ticket);
    MvcResult inventado = ver(beta, UUID.randomUUID().toString());

    assertThat(ajena.getResponse().getStatus()).isEqualTo(404);
    assertThat(inventado.getResponse().getStatus()).isEqualTo(404);
    assertThat(ajena.getResponse().getContentAsString())
        .isEqualTo(inventado.getResponse().getContentAsString());
    assertThat(ajena.getResponse().getContentAsString()).contains("no-encontrado");
  }

  @Test
  void transicionIlegalResponde409() throws Exception {
    String agente = token("agente@acme.test");
    String ticket = crear(agente, "MEDIA", "Consulta de horario");

    mvc.perform(
            post("/api/tickets/" + ticket + "/transicion")
                .header("Authorization", "Bearer " + agente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"CERRADO\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("conflicto"));

    mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + agente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ABIERTO"));
  }

  @Test
  void clienteRecibe403AlAsignar() throws Exception {
    String cliente = token("cliente@acme.test");
    String ticket = crear(cliente, "BAJA", "Duda de factura");

    mvc.perform(
            post("/api/tickets/" + ticket + "/asignacion")
                .header("Authorization", "Bearer " + cliente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"asignadoId\":\"22222222-2222-2222-2222-222222222222\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("prohibido"));

    mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + cliente))
        .andExpect(jsonPath("$.asignadoId").value(nullValue()));
  }

  @Test
  void clienteNoRecibeLaNotaInterna() throws Exception {
    String cliente = token("cliente@acme.test");
    String agente = token("agente@acme.test");
    String ticket = crear(cliente, "ALTA", "No entra");

    mvc.perform(
            post("/api/tickets/" + ticket + "/comentarios")
                .header("Authorization", "Bearer " + agente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cuerpo\":\"nota-interna-secreta\",\"interno\":true}"))
        .andExpect(status().isOk());

    String delCliente =
        mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + cliente))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(delCliente).doesNotContain("nota-interna-secreta");

    mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + agente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.comentarios[0].cuerpo").value("nota-interna-secreta"))
        .andExpect(jsonPath("$.comentarios[0].interno").value(true));
  }

  @Test
  void listaFiltradaYPaginada() throws Exception {
    String agente = token("agente@acme.test");
    String alta = crear(agente, "URGENTE", "Caida total");
    crear(agente, "BAJA", "Consulta menor");
    mvc.perform(
            post("/api/tickets/" + alta + "/asignacion")
                .header("Authorization", "Bearer " + agente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"asignadoId\":\"22222222-2222-2222-2222-222222222222\"}"))
        .andExpect(status().isOk());

    mvc.perform(
            get("/api/tickets")
                .header("Authorization", "Bearer " + agente)
                .param("estado", "ABIERTO")
                .param("prioridad", "URGENTE")
                .param("asignado", "22222222-2222-2222-2222-222222222222")
                .param("tamano", "1")
                .param("pagina", "0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tamano").value(1))
        .andExpect(jsonPath("$.pagina").value(0))
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.elementos[0].id").value(alta))
        .andExpect(jsonPath("$.elementos[1]").doesNotExist());
  }

  @Test
  void elTicketSeAbrePorSuCodigoSinDistinguirMayusculas() throws Exception {
    String cliente = token("cliente@acme.test");
    String id = crear(cliente, "ALTA", "Abrir por codigo");
    String codigo = JsonPath.read(ver(cliente, id).getResponse().getContentAsString(), "$.codigo");

    for (String referencia : new String[] {codigo, codigo.toLowerCase(), id}) {
      mvc.perform(get("/api/tickets/" + referencia).header("Authorization", "Bearer " + cliente))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(id))
          .andExpect(jsonPath("$.codigo").value(codigo));
    }
  }

  @Test
  void elCodigoDeOtraOrganizacionResponde404IgualQueUnoInexistente() throws Exception {
    String cliente = token("cliente@acme.test");
    String id = crear(cliente, "ALTA", "Codigo ajeno");
    String codigo = JsonPath.read(ver(cliente, id).getResponse().getContentAsString(), "$.codigo");
    String beta = tokenBeta();

    MvcResult ajeno = ver(beta, codigo);
    MvcResult inexistente = ver(beta, "RS-999999");

    assertThat(ajeno.getResponse().getStatus()).isEqualTo(404);
    assertThat(inexistente.getResponse().getStatus()).isEqualTo(404);
    assertThat(ajeno.getResponse().getContentAsString())
        .isEqualTo(inexistente.getResponse().getContentAsString());
  }

  @Test
  void unCodigoMalFormadoResponde404() throws Exception {
    String cliente = token("cliente@acme.test");

    for (String referencia : new String[] {"RS-", "abc", "RS-1x", "-1"}) {
      assertThat(ver(cliente, referencia).getResponse().getStatus())
          .as(referencia)
          .isEqualTo(404);
    }
  }

  private String crear(String token, String prioridad, String asunto) throws Exception {
    MvcResult resultado =
        mvc.perform(
                post("/api/tickets")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"categoriaId":"%s","prioridad":"%s","asunto":"%s","cuerpo":"detalle"}
                        """
                            .formatted(INCIDENCIA, prioridad, asunto)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
  }

  private String token(String email) throws Exception {
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

  private String tokenBeta() throws Exception {
    UUID org = UUID.randomUUID();
    UUID usuario = UUID.randomUUID();
    String hash =
        jdbc.queryForObject(
            "select hash_contrasena from usuario where email = ?",
            String.class,
            "cliente@acme.test");
    jdbc.update("insert into organizacion (id, nombre) values (?, 'Beta')", org);
    jdbc.update(
        """
        insert into usuario (id, organizacion_id, email, rol, hash_contrasena, fallos_seguidos)
        values (?, ?, ?, 'CLIENTE', ?, 0)
        """,
        usuario,
        org,
        "beta-" + usuario + "@beta.test",
        hash);
    return token("beta-" + usuario + "@beta.test");
  }

  private MvcResult ver(String token, String id) throws Exception {
    return mvc.perform(get("/api/tickets/" + id).header("Authorization", "Bearer " + token))
        .andReturn();
  }
}
