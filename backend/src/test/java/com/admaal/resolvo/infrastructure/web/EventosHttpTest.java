package com.admaal.resolvo.infrastructure.web;

import static com.admaal.resolvo.infrastructure.web.HttpAyuda.INCIDENCIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.admaal.resolvo.application.ProcesarEventos;
import com.admaal.resolvo.application.soporte.EmbebedorFalso;
import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import com.admaal.resolvo.infrastructure.ProcesadorDeEventos;
import com.jayway.jsonpath.JsonPath;
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

/** El outbox de punta a punta: la transicion escribe el evento y el procesador lo atiende. */
@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
@AutoConfigureMockMvc
@Import(IaDeMentira.class)
class EventosHttpTest {

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired EmbebedorFalso embebedor;
  @Autowired ProcesadorDeEventos procesador;

  private String agente;

  @BeforeEach
  void preparar() throws Exception {
    jdbc.update("delete from evento_procesado");
    jdbc.update("delete from evento_outbox");
    jdbc.update("delete from fragmento");
    embebedor.reiniciar();
    agente = HttpAyuda.token(mvc, "agente@acme.test");
  }

  private String nuevoTicket() throws Exception {
    return HttpAyuda.crearTicket(mvc, agente, INCIDENCIA, "Error al exportar informes");
  }

  private void transicionar(String ticket, String estado) throws Exception {
    mvc.perform(
            post("/api/tickets/" + ticket + "/transicion")
                .header("Authorization", "Bearer " + agente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"" + estado + "\"}"))
        .andExpect(status().isOk());
  }

  private int fragmentosDe(String ticket) {
    return jdbc.queryForObject(
        "select count(*) from fragmento where fuente = 'TICKET' and fuente_id = ?::uuid",
        Integer.class,
        ticket);
  }

  private int eventos(String ticket, String tipo, String estado) {
    return jdbc.queryForObject(
        "select count(*) from evento_outbox where ticket_id = ?::uuid and tipo = ? and estado = ?",
        Integer.class,
        ticket,
        tipo,
        estado);
  }

  @Test
  void resolverEscribeElEventoYElProcesadorIndexaElTicket() throws Exception {
    String ticket = nuevoTicket();

    transicionar(ticket, "RESUELTO");

    assertThat(eventos(ticket, "TICKET_RESUELTO", "PENDIENTE")).isEqualTo(1);
    assertThat(fragmentosDe(ticket)).isZero();

    ProcesarEventos.Resultado resultado = procesador.procesar();

    assertThat(resultado.hechos()).isEqualTo(1);
    assertThat(eventos(ticket, "TICKET_RESUELTO", "HECHO")).isEqualTo(1);
    assertThat(fragmentosDe(ticket)).isEqualTo(1);
  }

  @Test
  void procesarDosVecesNoReindexaNiGastaCuota() throws Exception {
    String ticket = nuevoTicket();
    transicionar(ticket, "RESUELTO");
    procesador.procesar();
    int llamadas = embebedor.llamadas();

    ProcesarEventos.Resultado segunda = procesador.procesar();

    assertThat(segunda.hechos()).isZero();
    assertThat(embebedor.llamadas()).isEqualTo(llamadas);
    assertThat(fragmentosDe(ticket)).isEqualTo(1);
  }

  @Test
  void reabrirUnTicketResueltoQuitaSusFragmentos() throws Exception {
    String ticket = nuevoTicket();
    transicionar(ticket, "RESUELTO");
    procesador.procesar();
    assertThat(fragmentosDe(ticket)).isEqualTo(1);

    transicionar(ticket, "ABIERTO");
    procesador.procesar();

    assertThat(eventos(ticket, "TICKET_REABIERTO", "HECHO")).isEqualTo(1);
    assertThat(fragmentosDe(ticket)).isZero();
  }

  private String comentar(String ticket, String cuerpo) throws Exception {
    String respuesta =
        mvc.perform(
                post("/api/tickets/" + ticket + "/comentarios")
                    .header("Authorization", "Bearer " + agente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"cuerpo\":\"%s\",\"interno\":false}".formatted(cuerpo)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(respuesta, "$.id");
  }

  private void ocultar(String ticket, String comentario) throws Exception {
    mvc.perform(
            post("/api/tickets/" + ticket + "/comentarios/" + comentario + "/ocultacion")
                .header("Authorization", "Bearer " + agente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"SPAM\"}"))
        .andExpect(status().isOk());
  }

  private void restaurar(String ticket, String comentario) throws Exception {
    String admin = HttpAyuda.token(mvc, "admin@acme.test");
    mvc.perform(
            delete("/api/tickets/" + ticket + "/comentarios/" + comentario + "/ocultacion")
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk());
  }

  private String textoIndexado(String ticket) {
    return jdbc.queryForObject(
        "select string_agg(texto, ' ') from fragmento where fuente = 'TICKET' and fuente_id = ?::uuid",
        String.class,
        ticket);
  }

  @Test
  void ocultarUnComentarioDeUnTicketResueltoQuitaSuTextoDelIndiceYRestaurarLoDevuelve()
      throws Exception {
    String ticket = nuevoTicket();
    String spam = comentar(ticket, "texto-de-spam");
    transicionar(ticket, "RESUELTO");
    procesador.procesar();
    assertThat(textoIndexado(ticket)).contains("texto-de-spam");

    ocultar(ticket, spam);

    assertThat(eventos(ticket, "COMENTARIO_MODERADO", "PENDIENTE")).isEqualTo(1);
    procesador.procesar();
    assertThat(eventos(ticket, "COMENTARIO_MODERADO", "HECHO")).isEqualTo(1);
    assertThat(textoIndexado(ticket)).doesNotContain("texto-de-spam");

    restaurar(ticket, spam);
    procesador.procesar();

    assertThat(textoIndexado(ticket)).contains("texto-de-spam");
  }

  @Test
  void ocultarEnUnTicketAbiertoNoDejaEvento() throws Exception {
    String ticket = nuevoTicket();
    String comentario = comentar(ticket, "texto-cualquiera");

    ocultar(ticket, comentario);

    assertThat(
            jdbc.queryForObject(
                "select count(*) from evento_outbox where ticket_id = ?::uuid", Integer.class, ticket))
        .isZero();
  }

  @Test
  void unaTransicionQueNoCambiaLaIndexacionNoDejaEvento() throws Exception {
    String ticket = nuevoTicket();

    transicionar(ticket, "PENDIENTE");

    assertThat(
            jdbc.queryForObject(
                "select count(*) from evento_outbox where ticket_id = ?::uuid", Integer.class, ticket))
        .isZero();
  }

  @Test
  void unaTransicionRechazadaNoDejaEvento() throws Exception {
    String ticket = nuevoTicket();

    mvc.perform(
            post("/api/tickets/" + ticket + "/transicion")
                .header("Authorization", "Bearer " + agente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"CERRADO\"}"))
        .andExpect(status().isConflict());

    assertThat(
            jdbc.queryForObject(
                "select count(*) from evento_outbox where ticket_id = ?::uuid", Integer.class, ticket))
        .isZero();
  }
}
