package com.admaal.resolvo.infrastructure.web;

import static com.admaal.resolvo.infrastructure.web.HttpAyuda.INCIDENCIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
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

/**
 * El camino de produccion, sin llamar a procesar() a mano: al confirmarse la transaccion que publica
 * el evento, el procesador se despierta en su hilo y lo atiende.
 */
@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
@AutoConfigureMockMvc
@Import(IaDeMentira.class)
class EventosAutomaticosHttpTest {

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
    registro.add("resolvo.ia.sincrona", () -> "false");
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  private String agente;

  @BeforeEach
  void preparar() throws Exception {
    jdbc.update("delete from evento_procesado");
    jdbc.update("delete from evento_outbox");
    jdbc.update("delete from fragmento");
    agente = HttpAyuda.token(mvc, "agente@acme.test");
  }

  private int fragmentosDe(String ticket) {
    return jdbc.queryForObject(
        "select count(*) from fragmento where fuente = 'TICKET' and fuente_id = ?::uuid",
        Integer.class,
        ticket);
  }

  private static void esperar(java.util.function.BooleanSupplier condicion)
      throws InterruptedException {
    long limite = System.currentTimeMillis() + 15_000;
    while (!condicion.getAsBoolean() && System.currentTimeMillis() < limite) {
      Thread.sleep(50);
    }
  }

  private void transicionar(String ticket, String estado) throws Exception {
    mvc.perform(
            post("/api/tickets/" + ticket + "/transicion")
                .header("Authorization", "Bearer " + agente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"" + estado + "\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void resolverYReabrirSeAtiendenSolosEnSegundoPlano() throws Exception {
    String ticket = HttpAyuda.crearTicket(mvc, agente, INCIDENCIA, "Error al exportar informes");

    transicionar(ticket, "RESUELTO");
    esperar(() -> fragmentosDe(ticket) == 1);
    assertThat(fragmentosDe(ticket)).isEqualTo(1);

    transicionar(ticket, "ABIERTO");
    esperar(() -> fragmentosDe(ticket) == 0);
    assertThat(fragmentosDe(ticket)).isZero();
    esperar(
        () ->
            jdbc.queryForObject(
                    "select count(*) from evento_outbox where ticket_id = ?::uuid and estado = 'HECHO'",
                    Integer.class,
                    ticket)
                == 2);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from evento_outbox where ticket_id = ?::uuid and estado = 'HECHO'",
                Integer.class,
                ticket))
        .isEqualTo(2);
  }
}
