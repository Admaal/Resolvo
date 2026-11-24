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

  @Test
  void unClienteNoAbreElTicketDeOtroClienteDeSuOrganizacion() throws Exception {
    String ajeno = crear(token("cliente@acme.test"), "ALTA", "Ticket de otro cliente");
    String codigo =
        JsonPath.read(ver(token("agente@acme.test"), ajeno).getResponse().getContentAsString(), "$.codigo");
    String otro = tokenOtroClienteDeAcme();

    MvcResult porId = ver(otro, ajeno);
    MvcResult porCodigo = ver(otro, codigo);
    MvcResult inexistente = ver(otro, "RS-999999");

    assertThat(porId.getResponse().getStatus()).isEqualTo(404);
    assertThat(porCodigo.getResponse().getStatus()).isEqualTo(404);
    assertThat(porId.getResponse().getContentAsString())
        .isEqualTo(inexistente.getResponse().getContentAsString());
    assertThat(porCodigo.getResponse().getContentAsString())
        .isEqualTo(inexistente.getResponse().getContentAsString());
    assertThat(porId.getResponse().getContentAsString()).doesNotContain("Ticket de otro cliente");
  }

  @Test
  void unClienteSoloListaLosTicketsQueHaAbierto() throws Exception {
    String agente = token("agente@acme.test");
    String delOtro = tokenOtroClienteDeAcme();
    String suyo = crear(delOtro, "BAJA", "Solo mio");
    String ajeno = crear(token("cliente@acme.test"), "BAJA", "Ajeno al otro cliente");

    String lista =
        mvc.perform(get("/api/tickets").header("Authorization", "Bearer " + delOtro).param("tamano", "50"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String autorId = JsonPath.read(ver(delOtro, suyo).getResponse().getContentAsString(), "$.autorId");

    assertThat((java.util.List<String>) JsonPath.read(lista, "$.elementos[*].id")).containsExactly(suyo);
    assertThat((java.util.List<String>) JsonPath.read(lista, "$.elementos[*].autorId"))
        .containsOnly(autorId);
    assertThat((Integer) JsonPath.read(lista, "$.total")).isEqualTo(1);

    mvc.perform(get("/api/tickets").header("Authorization", "Bearer " + agente).param("tamano", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThan(1)));
    mvc.perform(get("/api/tickets/" + ajeno).header("Authorization", "Bearer " + agente))
        .andExpect(status().isOk());
  }

  @Test
  void unAgenteYUnAdminAbrenElTicketDeUnCliente() throws Exception {
    String id = crear(token("cliente@acme.test"), "MEDIA", "Lo abre el equipo");

    for (String correo : new String[] {"agente@acme.test", "admin@acme.test"}) {
      mvc.perform(get("/api/tickets/" + id).header("Authorization", "Bearer " + token(correo)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(id));
    }
  }

  @Test
  void unClienteNoComentaEnElTicketDeOtroYRecibeElMismo404QueUnoInexistente() throws Exception {
    String ajeno = crear(token("cliente@acme.test"), "BAJA", "Comentario ajeno");
    String otro = tokenOtroClienteDeAcme();
    String cuerpo = "{\"cuerpo\":\"no deberia entrar\",\"interno\":false}";

    MvcResult sobreAjeno = enviar(post("/api/tickets/" + ajeno + "/comentarios"), otro, cuerpo);
    MvcResult sobreInexistente =
        enviar(post("/api/tickets/" + UUID.randomUUID() + "/comentarios"), otro, cuerpo);

    assertThat(sobreAjeno.getResponse().getStatus()).isEqualTo(404);
    assertThat(sobreAjeno.getResponse().getContentAsString())
        .isEqualTo(sobreInexistente.getResponse().getContentAsString());
    mvc.perform(get("/api/tickets/" + ajeno).header("Authorization", "Bearer " + token("agente@acme.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.comentarios").isEmpty());
  }

  @Test
  void unClienteComentaEnSuTicketYElEquipoEnCualquiera() throws Exception {
    String cliente = token("cliente@acme.test");
    String id = crear(cliente, "BAJA", "Comentarios permitidos");
    String cuerpo = "{\"cuerpo\":\"hola\",\"interno\":false}";

    for (String token : new String[] {cliente, token("agente@acme.test"), token("admin@acme.test")}) {
      assertThat(enviar(post("/api/tickets/" + id + "/comentarios"), token, cuerpo).getResponse().getStatus())
          .isEqualTo(200);
    }
  }

  @Test
  void unClienteRecibe404SobreUnTicketAjenoY403SobreElSuyoEnLosEndpointsDelEquipo() throws Exception {
    String duenio = token("cliente@acme.test");
    String otro = tokenOtroClienteDeAcme();
    String ajeno = crear(duenio, "BAJA", "Endpoints del equipo");
    String suyo = crear(otro, "BAJA", "Endpoints del equipo propio");
    String comentario = UUID.randomUUID().toString();
    String agente = "22222222-2222-2222-2222-222222222222";
    Object[][] casos = {
      {"POST", "/api/tickets/%s/transicion", "{\"estado\":\"RESUELTO\"}"},
      {"POST", "/api/tickets/%s/asignacion", "{\"asignadoId\":\"" + agente + "\"}"},
      {"POST", "/api/tickets/%s/comentarios/" + comentario + "/ocultacion", "{\"motivo\":\"SPAM\"}"},
      {"DELETE", "/api/tickets/%s/comentarios/" + comentario + "/ocultacion", ""},
    };

    for (Object[] caso : casos) {
      String ruta = (String) caso[1];
      MvcResult delOtro = enviar(construir((String) caso[0], ruta.formatted(ajeno)), otro, (String) caso[2]);
      MvcResult inexistente =
          enviar(construir((String) caso[0], ruta.formatted(UUID.randomUUID())), otro, (String) caso[2]);
      MvcResult delSuyo = enviar(construir((String) caso[0], ruta.formatted(suyo)), otro, (String) caso[2]);

      assertThat(delOtro.getResponse().getStatus()).as("ajeno " + ruta).isEqualTo(404);
      assertThat(delOtro.getResponse().getContentAsString())
          .as("cuerpo " + ruta)
          .isEqualTo(inexistente.getResponse().getContentAsString());
      assertThat(delSuyo.getResponse().getStatus()).as("propio " + ruta).isEqualTo(403);
    }
  }

  @Test
  void losEndpointsDeIaRespondenIgualAUnClienteSeaElTicketAjenoOPropio() throws Exception {
    String duenio = token("cliente@acme.test");
    String otro = tokenOtroClienteDeAcme();
    String ajeno = crear(duenio, "BAJA", "IA ajena");
    String suyo = crear(otro, "BAJA", "IA propia");

    for (String ruta :
        new String[] {"/api/tickets/%s/sugerencias", "/api/tickets/%s/clasificaciones"}) {
      assertThat(enviar(post(ruta.formatted(ajeno)), otro, "").getResponse().getStatus()).isEqualTo(403);
      assertThat(enviar(post(ruta.formatted(suyo)), otro, "").getResponse().getStatus()).isEqualTo(403);
      assertThat(enviar(get(ruta.formatted(ajeno) + "/actual"), otro, "").getResponse().getStatus())
          .isEqualTo(403);
    }
    String desconocido = UUID.randomUUID().toString();
    assertThat(enviar(post("/api/sugerencias/" + desconocido + "/aprobar"), otro, "{\"texto\":\"x\"}").getResponse().getStatus()).isEqualTo(403);
    assertThat(enviar(post("/api/sugerencias/" + desconocido + "/descartar"), otro, "").getResponse().getStatus()).isEqualTo(403);
    assertThat(enviar(post("/api/clasificaciones/" + desconocido + "/descarte"), otro, "").getResponse().getStatus()).isEqualTo(403);
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder construir(
      String metodo, String ruta) {
    return metodo.equals("DELETE")
        ? org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(ruta)
        : post(ruta);
  }

  private MvcResult enviar(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion,
      String token,
      String cuerpo)
      throws Exception {
    return mvc.perform(
            peticion
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
        .andReturn();
  }

  private String tokenOtroClienteDeAcme() throws Exception {
    UUID usuario = UUID.randomUUID();
    String hash =
        jdbc.queryForObject(
            "select hash_contrasena from usuario where email = ?",
            String.class,
            "cliente@acme.test");
    jdbc.update(
        """
        insert into usuario (id, organizacion_id, email, rol, hash_contrasena, fallos_seguidos)
        values (?, '11111111-1111-1111-1111-111111111111', ?, 'CLIENTE', ?, 0)
        """,
        usuario,
        "otro-" + usuario + "@acme.test",
        hash);
    return token("otro-" + usuario + "@acme.test");
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
