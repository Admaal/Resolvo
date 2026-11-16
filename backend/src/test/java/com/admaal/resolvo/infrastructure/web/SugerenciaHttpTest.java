package com.admaal.resolvo.infrastructure.web;

import static com.admaal.resolvo.infrastructure.web.HttpAyuda.INCIDENCIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.admaal.resolvo.application.IaNoDisponible;
import com.admaal.resolvo.application.soporte.EmbebedorFalso;
import com.admaal.resolvo.application.soporte.RedactorFalso;
import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import com.admaal.resolvo.infrastructure.web.HttpAyuda.Organizacion;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
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
class SugerenciaHttpTest {

  private static final String PRIMER_ARTICULO = "66666666-6666-6666-6666-666666666661";

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired EmbebedorFalso embebedor;
  @Autowired RedactorFalso redactor;

  private String agente;
  private String cliente;
  private String admin;

  @BeforeEach
  void preparar() throws Exception {
    jdbc.update("delete from cita");
    jdbc.update("delete from sugerencia");
    jdbc.update("delete from fragmento");
    embebedor.reiniciar();
    redactor.reiniciar();
    agente = HttpAyuda.token(mvc, "agente@acme.test");
    cliente = HttpAyuda.token(mvc, "cliente@acme.test");
    admin = HttpAyuda.token(mvc, "admin@acme.test");
    mvc.perform(post("/api/conocimiento/reindexar").header("Authorization", "Bearer " + admin))
        .andExpect(status().isNoContent());
    embebedor.reiniciar();
  }

  private String nuevoTicket() throws Exception {
    return HttpAyuda.crearTicket(mvc, agente, INCIDENCIA, "Error al exportar informes");
  }

  private ResultActions sugerir(String token, String ticket) throws Exception {
    return mvc.perform(
        post("/api/tickets/" + ticket + "/sugerencias").header("Authorization", "Bearer " + token));
  }

  private String sugerenciaId(String ticket) throws Exception {
    String cuerpo =
        sugerir(agente, ticket).andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
    return JsonPath.read(cuerpo, "$.id");
  }

  private ResultActions aprobar(String token, String sugerencia, String texto) throws Exception {
    return mvc.perform(
        post("/api/sugerencias/" + sugerencia + "/aprobar")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"texto\":\"%s\"}".formatted(texto)));
  }

  @Test
  void sugerirDevuelve201ConBorradorYCitas() throws Exception {
    String ticket = nuevoTicket();

    sugerir(agente, ticket)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("PENDIENTE"))
        .andExpect(jsonPath("$.borrador").value("Borrador sugerido"))
        .andExpect(jsonPath("$.ticketId").value(ticket))
        .andExpect(jsonPath("$.citas[0].orden").value(1))
        .andExpect(jsonPath("$.citas[0].fuente").value(org.hamcrest.Matchers.oneOf("ARTICULO", "TICKET")))
        .andExpect(jsonPath("$.citas[0].titulo").isNotEmpty());
  }

  @Test
  void elDobleClicDevuelveLaMismaSugerenciaSinLlamarDeNuevoAlProveedor() throws Exception {
    String ticket = nuevoTicket();
    String primera =
        JsonPath.read(
            sugerir(agente, ticket).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
            "$.id");

    sugerir(agente, ticket).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(primera));

    assertThat(redactor.llamadas()).isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from sugerencia where ticket_id = ?::uuid", Integer.class, ticket))
        .isEqualTo(1);
  }

  @Test
  void laSugerenciaActualSeConsultaYSinSugerenciaEs404() throws Exception {
    String ticket = nuevoTicket();
    mvc.perform(get("/api/tickets/" + ticket + "/sugerencias/actual").header("Authorization", "Bearer " + agente))
        .andExpect(status().isNotFound());

    String id = sugerenciaId(ticket);

    mvc.perform(get("/api/tickets/" + ticket + "/sugerencias/actual").header("Authorization", "Bearer " + agente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.citas[0].titulo").isNotEmpty());
  }

  @Test
  void clienteRecibe403EnTodosLosEndpointsDeSugerencias() throws Exception {
    String ticket = nuevoTicket();
    String id = sugerenciaId(ticket);

    sugerir(cliente, ticket).andExpect(status().isForbidden());
    mvc.perform(get("/api/tickets/" + ticket + "/sugerencias/actual").header("Authorization", "Bearer " + cliente))
        .andExpect(status().isForbidden());
    aprobar(cliente, id, "hola").andExpect(status().isForbidden());
    mvc.perform(post("/api/sugerencias/" + id + "/descartar").header("Authorization", "Bearer " + cliente))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/articulos/" + PRIMER_ARTICULO).header("Authorization", "Bearer " + cliente))
        .andExpect(status().isForbidden());

    mvc.perform(get("/api/tickets/" + ticket + "/sugerencias/actual").header("Authorization", "Bearer " + agente))
        .andExpect(jsonPath("$.estado").value("PENDIENTE"));
  }

  @Test
  void otraOrganizacionRecibe404SinFiltrarQueExiste() throws Exception {
    String ticket = nuevoTicket();
    String id = sugerenciaId(ticket);
    Organizacion ajena = HttpAyuda.organizacionNueva(mvc, jdbc);

    var ajeno = sugerir(ajena.agente(), ticket).andExpect(status().isNotFound()).andReturn();
    var inventado =
        sugerir(ajena.agente(), UUID.randomUUID().toString()).andExpect(status().isNotFound()).andReturn();
    assertThat(ajeno.getResponse().getContentAsString()).isEqualTo(inventado.getResponse().getContentAsString());
    mvc.perform(get("/api/tickets/" + ticket + "/sugerencias/actual").header("Authorization", "Bearer " + ajena.agente()))
        .andExpect(status().isNotFound());
    aprobar(ajena.agente(), id, "hola").andExpect(status().isNotFound());
    mvc.perform(post("/api/sugerencias/" + id + "/descartar").header("Authorization", "Bearer " + ajena.agente()))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/articulos/" + PRIMER_ARTICULO).header("Authorization", "Bearer " + ajena.agente()))
        .andExpect(status().isNotFound());
  }

  @Test
  void laRedactoraNoRecibeFragmentosDeOtraOrganizacion() throws Exception {
    Organizacion ajena = HttpAyuda.organizacionNueva(mvc, jdbc);
    String ticketAjeno = HttpAyuda.crearTicket(mvc, ajena.agente(), ajena.categoriaId(), "Asunto ajeno");

    sugerir(ajena.agente(), ticketAjeno).andExpect(status().isUnprocessableEntity());

    assertThat(redactor.llamadas()).isZero();
  }

  @Test
  void sinFuentesResponde422YNoLlamaAlProveedor() throws Exception {
    Organizacion nueva = HttpAyuda.organizacionNueva(mvc, jdbc);
    String ticket = HttpAyuda.crearTicket(mvc, nueva.agente(), nueva.categoriaId(), "Sin base");

    sugerir(nueva.agente(), ticket)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.error").value("sin-fuentes"));

    assertThat(redactor.llamadas()).isZero();
  }

  @Test
  void aprobarPublicaUnComentarioVisibleAlCliente() throws Exception {
    String ticket = nuevoTicket();
    String id = sugerenciaId(ticket);

    aprobar(agente, id, "Respuesta final editada")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cuerpo").value("Respuesta final editada"))
        .andExpect(jsonPath("$.interno").value(false));

    mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + cliente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.comentarios[0].cuerpo").value("Respuesta final editada"))
        .andExpect(jsonPath("$.comentarios[0].interno").value(false));
    assertThat(jdbc.queryForObject("select estado from sugerencia where id = ?::uuid", String.class, id))
        .isEqualTo("APROBADA");
  }

  @Test
  void dobleAprobacionEs409YNoDuplicaElComentario() throws Exception {
    String ticket = nuevoTicket();
    String id = sugerenciaId(ticket);
    aprobar(agente, id, "uno").andExpect(status().isOk());

    aprobar(agente, id, "dos").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("conflicto"));

    assertThat(jdbc.queryForObject("select count(*) from comentario where ticket_id = ?::uuid", Integer.class, ticket))
        .isEqualTo(1);
  }

  @Test
  void unTextoVacioNoPublicaNiResuelve() throws Exception {
    String ticket = nuevoTicket();
    String id = sugerenciaId(ticket);

    aprobar(agente, id, "   ").andExpect(status().isUnprocessableEntity());

    assertThat(jdbc.queryForObject("select estado from sugerencia where id = ?::uuid", String.class, id))
        .isEqualTo("PENDIENTE");
    assertThat(jdbc.queryForObject("select count(*) from comentario where ticket_id = ?::uuid", Integer.class, ticket))
        .isZero();
  }

  @Test
  void descartarPermiteGenerarOtraSugerencia() throws Exception {
    String ticket = nuevoTicket();
    String primera = sugerenciaId(ticket);

    mvc.perform(post("/api/sugerencias/" + primera + "/descartar").header("Authorization", "Bearer " + agente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("DESCARTADA"));

    sugerir(agente, ticket)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(primera)));
    assertThat(redactor.llamadas()).isEqualTo(2);
  }

  @Test
  void conLaCuotaAgotadaResponde503YNoDejaSugerencia() throws Exception {
    String ticket = nuevoTicket();
    redactor.fallando(new IaNoDisponible("ia-cuota-agotada"));

    sugerir(agente, ticket)
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("ia-cuota-agotada"));

    mvc.perform(get("/api/tickets/" + ticket + "/sugerencias/actual").header("Authorization", "Bearer " + agente))
        .andExpect(status().isNotFound());
  }

  @Test
  void elPersonalLeeElArticuloCitado() throws Exception {
    mvc.perform(get("/api/articulos/" + PRIMER_ARTICULO).header("Authorization", "Bearer " + agente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(PRIMER_ARTICULO))
        .andExpect(jsonPath("$.titulo").value("Como restablecer tu contrasena"))
        .andExpect(jsonPath("$.cuerpo").isNotEmpty());
  }

  @Test
  void alResolverUnTicketQuedaIndexadoParaLasSiguientesSugerencias() throws Exception {
    String ticket = nuevoTicket();

    mvc.perform(
            post("/api/tickets/" + ticket + "/transicion")
                .header("Authorization", "Bearer " + agente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"RESUELTO\"}"))
        .andExpect(status().isOk());

    assertThat(
            jdbc.queryForObject(
                "select count(*) from fragmento where fuente = 'TICKET' and fuente_id = ?::uuid",
                Integer.class,
                ticket))
        .isEqualTo(1);
  }

  @Test
  void unTicketAbiertoNoSeIndexa() throws Exception {
    String ticket = nuevoTicket();

    mvc.perform(
            post("/api/tickets/" + ticket + "/transicion")
                .header("Authorization", "Bearer " + agente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"PENDIENTE\"}"))
        .andExpect(status().isOk());

    assertThat(
            jdbc.queryForObject(
                "select count(*) from fragmento where fuente_id = ?::uuid", Integer.class, ticket))
        .isZero();
  }
}
