package com.admaal.resolvo.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.ResultActions;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
@AutoConfigureMockMvc
class ModeracionHttpTest {

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  private String cliente;
  private String agente;
  private String admin;
  private String ticket;

  private void preparar() throws Exception {
    cliente = HttpAyuda.token(mvc, "cliente@acme.test");
    agente = HttpAyuda.token(mvc, "agente@acme.test");
    admin = HttpAyuda.token(mvc, "admin@acme.test");
    ticket = HttpAyuda.crearTicket(mvc, cliente, HttpAyuda.INCIDENCIA, "Moderacion " + UUID.randomUUID());
  }

  private String comentar(String token, String cuerpo) throws Exception {
    String respuesta =
        mvc.perform(
                post("/api/tickets/" + ticket + "/comentarios")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"cuerpo\":\"%s\",\"interno\":false}".formatted(cuerpo)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(respuesta, "$.id");
  }

  private ResultActions ocultar(String token, String comentario, String motivo) throws Exception {
    return mvc.perform(
        post("/api/tickets/" + ticket + "/comentarios/" + comentario + "/ocultacion")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"motivo\":\"%s\"}".formatted(motivo)));
  }

  private ResultActions restaurar(String token, String comentario) throws Exception {
    return mvc.perform(
        delete("/api/tickets/" + ticket + "/comentarios/" + comentario + "/ocultacion")
            .header("Authorization", "Bearer " + token));
  }

  private ResultActions ver(String token) throws Exception {
    return mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + token));
  }

  @Test
  void elAgenteOcultaYElClienteVeElComentarioRetiradoSinTexto() throws Exception {
    preparar();
    String spam = comentar(cliente, "texto-de-spam");

    ocultar(agente, spam, "SPAM")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.retirado").value(true))
        .andExpect(jsonPath("$.oculto.motivo").value("SPAM"));

    ver(cliente)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.comentarios[0].id").value(spam))
        .andExpect(jsonPath("$.comentarios[0].retirado").value(true))
        .andExpect(jsonPath("$.comentarios[0].cuerpo").value(nullValue()))
        .andExpect(jsonPath("$.comentarios[0].oculto.motivo").value(nullValue()))
        .andExpect(jsonPath("$.comentarios[0].oculto.porId").value(nullValue()));
    ver(agente)
        .andExpect(jsonPath("$.comentarios[0].retirado").value(true))
        .andExpect(jsonPath("$.comentarios[0].cuerpo").value("texto-de-spam"))
        .andExpect(jsonPath("$.comentarios[0].oculto.motivo").value("SPAM"));
  }

  @Test
  void ocultarYGuardarConservaElCuerpoYNoBorraNada() throws Exception {
    preparar();
    String spam = comentar(cliente, "se-conserva");
    ocultar(agente, spam, "OFENSIVO").andExpect(status().isOk());

    comentar(agente, "otra-respuesta");

    Integer filas =
        jdbc.queryForObject(
            "select count(*) from comentario where ticket_id = ?::uuid", Integer.class, ticket);
    String guardado =
        jdbc.queryForObject(
            "select cuerpo || '|' || oculto_motivo from comentario where id = ?::uuid",
            String.class,
            spam);
    assertThat(filas).isEqualTo(2);
    assertThat(guardado).isEqualTo("se-conserva|OFENSIVO");
  }

  @Test
  void unClienteNoPuedeOcultar() throws Exception {
    preparar();
    String comentario = comentar(cliente, "hola");

    ocultar(cliente, comentario, "SPAM")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("prohibido"));
    ver(agente).andExpect(jsonPath("$.comentarios[0].retirado").value(false));
  }

  @Test
  void soloAdminRestaura() throws Exception {
    preparar();
    String comentario = comentar(cliente, "feo");
    ocultar(agente, comentario, "OFENSIVO").andExpect(status().isOk());

    restaurar(agente, comentario).andExpect(status().isForbidden());
    restaurar(cliente, comentario).andExpect(status().isForbidden());
    ver(agente).andExpect(jsonPath("$.comentarios[0].retirado").value(true));

    restaurar(admin, comentario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.retirado").value(false))
        .andExpect(jsonPath("$.oculto").value(nullValue()));
    ver(cliente)
        .andExpect(jsonPath("$.comentarios[0].retirado").value(false))
        .andExpect(jsonPath("$.comentarios[0].cuerpo").value("feo"));
  }

  @Test
  void ocultarDosVecesConservaLaPrimeraModeracion() throws Exception {
    preparar();
    String comentario = comentar(cliente, "feo");
    ocultar(agente, comentario, "OFENSIVO").andExpect(status().isOk());

    ocultar(admin, comentario, "SPAM")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.oculto.motivo").value("OFENSIVO"));
  }

  @Test
  void otraOrganizacionNoEncuentraElComentario() throws Exception {
    preparar();
    String comentario = comentar(cliente, "hola");
    var ajena = HttpAyuda.organizacionNueva(mvc, jdbc);

    ocultar(ajena.agente(), comentario, "SPAM")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("no-encontrado"));
    ver(agente).andExpect(jsonPath("$.comentarios[0].retirado").value(false));
  }

  @Test
  void unMotivoDesconocidoDa422() throws Exception {
    preparar();
    String comentario = comentar(cliente, "hola");

    ocultar(agente, comentario, "ABURRIDO")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.error").value("invalido"));
  }

  @Test
  void unClienteConMasDeTresEnlacesDa422YUnAgenteNo() throws Exception {
    preparar();
    String cuatro = "http://a.test http://b.test http://c.test http://d.test";

    mvc.perform(
            post("/api/tickets/" + ticket + "/comentarios")
                .header("Authorization", "Bearer " + cliente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cuerpo\":\"%s\",\"interno\":false}".formatted(cuatro)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.error").value("demasiados-enlaces"));

    comentar(agente, cuatro);
  }
}
