package com.admaal.resolvo.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
@SpringBootTest(classes = ResolvoApplication.class)
@AutoConfigureMockMvc
@Import(IaDeMentira.class)
class MetricasHttpTest {

  private static final String ACME = "11111111-1111-1111-1111-111111111111";

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  private String admin;
  private String agente;
  private String cliente;
  private String otraOrganizacion;

  @BeforeEach
  void preparar() throws Exception {
    jdbc.update("delete from llamada_ia");
    admin = HttpAyuda.token(mvc, "admin@acme.test");
    agente = HttpAyuda.token(mvc, "agente@acme.test");
    cliente = HttpAyuda.token(mvc, "cliente@acme.test");
    otraOrganizacion = HttpAyuda.organizacionNueva(mvc, jdbc).agente();
  }

  private void llamada(
      String organizacion, String operacion, String modelo, int entrada, int salida, long ms,
      boolean exito, Instant cuando) {
    jdbc.update(
        """
        insert into llamada_ia
          (id, organizacion_id, operacion, modelo, tokens_entrada, tokens_salida, milisegundos, exito, ocurrida_en)
        values (?, ?::uuid, ?, ?, ?, ?, ?, ?, ?)
        """,
        UUID.randomUUID(),
        organizacion,
        operacion,
        modelo,
        entrada,
        salida,
        ms,
        exito,
        Timestamp.from(cuando));
  }

  /** La organizacion va en el claim del JWT: se lee del cuerpo del token sin verificar la firma. */
  private static String organizacionDe(String token) {
    String cuerpo =
        new String(
            java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]),
            java.nio.charset.StandardCharsets.UTF_8);
    return com.jayway.jsonpath.JsonPath.read(cuerpo, "$.organizacionId");
  }

  @Test
  void elAdminVeElResumenDeSuOrganizacion() throws Exception {
    Instant ahora = Instant.now();
    llamada(ACME, "sugerir", "gemini-3.1-flash-lite", 1_000_000, 0, 2_000, true, ahora.minusSeconds(60));
    llamada(ACME, "sugerir", "gemini-3.1-flash-lite", 0, 0, 15_000, false, ahora.minusSeconds(30));
    llamada(ACME, "clasificar", "gemini-flash-lite-latest", 300, 40, 900, true, ahora.minusSeconds(10));

    mvc.perform(get("/api/metricas/ia").header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.llamadas").value(3))
        .andExpect(jsonPath("$.fallos").value(1))
        .andExpect(jsonPath("$.tokensEntrada").value(1_000_300))
        .andExpect(jsonPath("$.tokensSalida").value(40))
        .andExpect(jsonPath("$.filas.length()").value(2))
        .andExpect(jsonPath("$.filas[0].operacion").value("clasificar"))
        .andExpect(jsonPath("$.filas[0].costeUsd").doesNotExist())
        .andExpect(jsonPath("$.filas[1].operacion").value("sugerir"))
        .andExpect(jsonPath("$.filas[1].llamadas").value(2))
        .andExpect(jsonPath("$.filas[1].fallos").value(1))
        .andExpect(jsonPath("$.filas[1].p95Ms").value(15_000))
        .andExpect(jsonPath("$.filas[1].costeUsd").value(0.25))
        // el alias sin tarifa deja el total desconocido, no cero
        .andExpect(jsonPath("$.costeUsd").doesNotExist());
  }

  @Test
  void nuncaMuestraLlamadasDeOtraOrganizacion() throws Exception {
    String otra = organizacionDe(otraOrganizacion);
    Instant ahora = Instant.now();
    llamada(ACME, "sugerir", "gemini-3.1-flash-lite", 10, 1, 100, true, ahora.minusSeconds(5));
    llamada(otra, "sugerir", "gemini-3.1-flash-lite", 999_999, 999, 100, true, ahora.minusSeconds(5));

    mvc.perform(get("/api/metricas/ia").header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.llamadas").value(1))
        .andExpect(jsonPath("$.tokensEntrada").value(10));
  }

  @Test
  void agenteYClienteRecibenProhibido() throws Exception {
    mvc.perform(get("/api/metricas/ia").header("Authorization", "Bearer " + agente))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/metricas/ia").header("Authorization", "Bearer " + cliente))
        .andExpect(status().isForbidden());
  }

  @Test
  void sinSesionRespondeNoAutorizado() throws Exception {
    mvc.perform(get("/api/metricas/ia")).andExpect(status().isUnauthorized());
  }

  @Test
  void elRangoPorDefectoSonLosUltimosSieteDiasYSePuedeAcotar() throws Exception {
    Instant ahora = Instant.now();
    llamada(ACME, "sugerir", "gemini-3.1-flash-lite", 1, 1, 100, true, ahora.minusSeconds(3600L * 24 * 10));
    llamada(ACME, "sugerir", "gemini-3.1-flash-lite", 2, 2, 100, true, ahora.minusSeconds(3600L * 24 * 2));

    mvc.perform(get("/api/metricas/ia").header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.llamadas").value(1));

    mvc.perform(
            get("/api/metricas/ia")
                .param("desde", ahora.minusSeconds(3600L * 24 * 30).toString())
                .param("hasta", ahora.toString())
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.llamadas").value(2));
  }

  @Test
  void unRangoAlRevesOIlegibleEsUnprocessable() throws Exception {
    Instant ahora = Instant.now();
    mvc.perform(
            get("/api/metricas/ia")
                .param("desde", ahora.toString())
                .param("hasta", ahora.minusSeconds(60).toString())
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.error").value("rango-invalido"));
    mvc.perform(
            get("/api/metricas/ia")
                .param("desde", "ayer")
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void sinLlamadasElResumenVieneVacio() throws Exception {
    mvc.perform(get("/api/metricas/ia").header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.llamadas").value(0))
        .andExpect(jsonPath("$.filas.length()").value(0))
        .andExpect(jsonPath("$.costeUsd").value(0));
  }
}
