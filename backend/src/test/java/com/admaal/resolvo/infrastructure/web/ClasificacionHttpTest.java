package com.admaal.resolvo.infrastructure.web;

import static com.admaal.resolvo.infrastructure.web.HttpAyuda.INCIDENCIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.admaal.resolvo.application.IaNoDisponible;
import com.admaal.resolvo.application.soporte.ClasificadorFalso;
import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import com.admaal.resolvo.infrastructure.web.HttpAyuda.Organizacion;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
@AutoConfigureMockMvc
@Import(IaDeMentira.class)
class ClasificacionHttpTest {

  private static final String CONSULTA = "33333333-3333-3333-3333-333333333332";

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ClasificadorFalso clasificador;

  private String agente;
  private String cliente;

  @BeforeEach
  void preparar() throws Exception {
    jdbc.update("delete from clasificacion");
    clasificador.reiniciar();
    clasificador.respondiendo("Consulta", Prioridad.URGENTE, "Pregunta por una factura");
    agente = HttpAyuda.token(mvc, "agente@acme.test");
    cliente = HttpAyuda.token(mvc, "cliente@acme.test");
  }

  private String nuevoTicket() throws Exception {
    return HttpAyuda.crearTicket(mvc, agente, INCIDENCIA, "Factura duplicada");
  }

  private ResultActions sugerir(String token, String ticket) throws Exception {
    return mvc.perform(
        post("/api/tickets/" + ticket + "/clasificaciones").header("Authorization", "Bearer " + token));
  }

  private String clasificacionId(String ticket) throws Exception {
    String cuerpo =
        sugerir(agente, ticket).andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
    return JsonPath.read(cuerpo, "$.id");
  }

  private ResultActions aplicar(String token, String clasificacion, String categoria, String prioridad)
      throws Exception {
    return mvc.perform(
        post("/api/clasificaciones/" + clasificacion + "/aplicacion")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"categoriaId\":\"%s\",\"prioridad\":\"%s\"}".formatted(categoria, prioridad)));
  }

  @Test
  void agenteSugiereYRecibeLaPropuestaPendiente() throws Exception {
    String ticket = nuevoTicket();

    sugerir(agente, ticket)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ticketId").value(ticket))
        .andExpect(jsonPath("$.categoriaId").value(CONSULTA))
        .andExpect(jsonPath("$.prioridad").value("URGENTE"))
        .andExpect(jsonPath("$.motivo").value("Pregunta por una factura"))
        .andExpect(jsonPath("$.estado").value("PENDIENTE"))
        .andExpect(jsonPath("$.modelo").value("clasificador-falso"));
  }

  @Test
  void elDobleClicDevuelveLaMismaYNoGastaOtraLlamada() throws Exception {
    String ticket = nuevoTicket();
    String primera = clasificacionId(ticket);

    sugerir(agente, ticket).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(primera));

    assertThat(clasificador.llamadas()).isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from clasificacion", Integer.class)).isEqualTo(1);
  }

  @Test
  void laActualDevuelveLaPendienteYSinPendienteEs404() throws Exception {
    String ticket = nuevoTicket();
    mvc.perform(get("/api/tickets/" + ticket + "/clasificaciones/actual").header("Authorization", "Bearer " + agente))
        .andExpect(status().isNotFound());

    String id = clasificacionId(ticket);

    mvc.perform(get("/api/tickets/" + ticket + "/clasificaciones/actual").header("Authorization", "Bearer " + agente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id));
  }

  @Test
  void clienteRecibe403EnTodasLasOperacionesYNoSeLlamaAlModelo() throws Exception {
    String ticket = nuevoTicket();
    String id = clasificacionId(ticket);
    clasificador.reiniciar();

    sugerir(cliente, ticket).andExpect(status().isForbidden());
    mvc.perform(get("/api/tickets/" + ticket + "/clasificaciones/actual").header("Authorization", "Bearer " + cliente))
        .andExpect(status().isForbidden());
    aplicar(cliente, id, CONSULTA, "ALTA").andExpect(status().isForbidden());
    mvc.perform(post("/api/clasificaciones/" + id + "/descarte").header("Authorization", "Bearer " + cliente))
        .andExpect(status().isForbidden());

    assertThat(clasificador.llamadas()).isZero();
    mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + agente))
        .andExpect(jsonPath("$.prioridad").value("ALTA"));
  }

  @Test
  void otraOrganizacionRecibe404SinFiltrarQueExiste() throws Exception {
    String ticket = nuevoTicket();
    String id = clasificacionId(ticket);
    Organizacion otra = HttpAyuda.organizacionNueva(mvc, jdbc);

    sugerir(otra.agente(), ticket).andExpect(status().isNotFound());
    aplicar(otra.agente(), id, otra.categoriaId(), "BAJA").andExpect(status().isNotFound());
    mvc.perform(post("/api/clasificaciones/" + id + "/descarte").header("Authorization", "Bearer " + otra.agente()))
        .andExpect(status().isNotFound());
  }

  @Test
  void aplicarCambiaCategoriaPrioridadYVencimiento() throws Exception {
    String ticket = nuevoTicket();
    String id = clasificacionId(ticket);
    String antes =
        mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + agente))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Instant abiertoEn = Instant.parse(JsonPath.read(antes, "$.abiertoEn"));

    String cuerpo =
        aplicar(agente, id, CONSULTA, "URGENTE")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categoriaId").value(CONSULTA))
            .andExpect(jsonPath("$.prioridad").value("URGENTE"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Consulta + URGENTE: 2 h de primera respuesta desde la apertura (PoliticaSla).
    Instant venceEl = Instant.parse(JsonPath.read(cuerpo, "$.venceEl"));
    assertThat(Duration.between(abiertoEn, venceEl)).isEqualTo(Duration.ofHours(2));
    mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + agente))
        .andExpect(jsonPath("$.categoriaId").value(CONSULTA))
        .andExpect(jsonPath("$.prioridad").value("URGENTE"));
    assertThat(jdbc.queryForObject("select estado from clasificacion where id = ?::uuid", String.class, id))
        .isEqualTo("APLICADA");
  }

  @Test
  void elAgentePuedeAplicarOtraCosaQueLaPropuesta() throws Exception {
    String ticket = nuevoTicket();
    String id = clasificacionId(ticket);

    aplicar(agente, id, INCIDENCIA, "BAJA")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categoriaId").value(INCIDENCIA))
        .andExpect(jsonPath("$.prioridad").value("BAJA"));
  }

  @Test
  void aplicarConLaCategoriaDeOtraOrganizacionEs404YNoCambiaNada() throws Exception {
    String ticket = nuevoTicket();
    String id = clasificacionId(ticket);
    Organizacion otra = HttpAyuda.organizacionNueva(mvc, jdbc);

    aplicar(agente, id, otra.categoriaId(), "BAJA").andExpect(status().isNotFound());

    mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + agente))
        .andExpect(jsonPath("$.categoriaId").value(INCIDENCIA))
        .andExpect(jsonPath("$.prioridad").value("ALTA"));
    assertThat(jdbc.queryForObject("select estado from clasificacion where id = ?::uuid", String.class, id))
        .isEqualTo("PENDIENTE");
  }

  @Test
  void aplicarConElCuerpoIncompletoEs422YNoCambiaNada() throws Exception {
    String ticket = nuevoTicket();
    String id = clasificacionId(ticket);

    for (String cuerpo :
        new String[] {"{}", "{\"categoriaId\":\"%s\"}".formatted(CONSULTA), "{\"prioridad\":\"ALTA\"}"}) {
      mvc.perform(
              post("/api/clasificaciones/" + id + "/aplicacion")
                  .header("Authorization", "Bearer " + agente)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(cuerpo))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.error").value("invalido"));
    }

    mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + agente))
        .andExpect(jsonPath("$.prioridad").value("ALTA"));
    assertThat(jdbc.queryForObject("select estado from clasificacion where id = ?::uuid", String.class, id))
        .isEqualTo("PENDIENTE");
  }

  @Test
  void aplicarDosVecesEs409() throws Exception {
    String id = clasificacionId(nuevoTicket());
    aplicar(agente, id, CONSULTA, "ALTA").andExpect(status().isOk());

    aplicar(agente, id, CONSULTA, "ALTA").andExpect(status().isConflict());
  }

  @Test
  void descartarNoTocaElTicketYPermiteProponerOtra() throws Exception {
    String ticket = nuevoTicket();
    String id = clasificacionId(ticket);

    mvc.perform(post("/api/clasificaciones/" + id + "/descarte").header("Authorization", "Bearer " + agente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("DESCARTADA"));

    mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + agente))
        .andExpect(jsonPath("$.prioridad").value("ALTA"));
    sugerir(agente, ticket).andExpect(status().isCreated());
    assertThat(clasificador.llamadas()).isEqualTo(2);
  }

  @Test
  void unTicketResueltoNoSeClasificaEs409SinGastarCuota() throws Exception {
    String ticket = nuevoTicket();
    mvc.perform(
            post("/api/tickets/" + ticket + "/transicion")
                .header("Authorization", "Bearer " + agente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"RESUELTO\"}"))
        .andExpect(status().isOk());
    clasificador.reiniciar();

    sugerir(agente, ticket).andExpect(status().isConflict());

    assertThat(clasificador.llamadas()).isZero();
  }

  @Test
  void unaPropuestaInvalidaDelModeloEs503ConSuMotivoYNoSeGuarda() throws Exception {
    clasificador.respondiendo("Facturacion", Prioridad.ALTA, "motivo");

    sugerir(agente, nuevoTicket())
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("clasificacion-invalida"));

    assertThat(jdbc.queryForObject("select count(*) from clasificacion", Integer.class)).isZero();
  }

  @Test
  void siElProveedorFallaEs503YElRestoDeLaAppSigue() throws Exception {
    clasificador.fallando(new IaNoDisponible("ia-cuota-agotada"));
    String ticket = nuevoTicket();

    sugerir(agente, ticket)
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("ia-cuota-agotada"));

    mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + agente))
        .andExpect(status().isOk());
  }
}
