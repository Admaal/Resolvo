package com.admaal.resolvo.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import java.util.UUID;
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
@SpringBootTest(
    classes = ResolvoApplication.class,
    properties = {"resolvo.limites.ia-por-minuto=10", "resolvo.proxy.clave-compartida=secreto-de-prueba"})
@AutoConfigureMockMvc
@Import(IaDeMentira.class)
class LimitesHttpTest {

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  private ResultActions entrar(String email, String contrasena, String cliente) throws Exception {
    return mvc.perform(
        post("/api/sesion")
            .header("CF-Connecting-IP", cliente)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"%s\",\"contrasena\":\"%s\"}".formatted(email, contrasena)));
  }

  @Test
  void seisFallosDeLoginDanUn429ConRetryAfter() throws Exception {
    for (int intento = 0; intento < 5; intento++) {
      entrar("agente@acme.test", "mala", "203.0.113.10").andExpect(status().isUnauthorized());
    }

    var respuesta =
        entrar("agente@acme.test", "demo-resolvo", "203.0.113.10")
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").value("demasiados-intentos"))
            .andReturn()
            .getResponse();

    assertThat(Long.parseLong(respuesta.getHeader("Retry-After"))).isBetween(1L, 900L);
  }

  @Test
  void otroClienteSigueEntrandoConLaMismaCuenta() throws Exception {
    for (int intento = 0; intento < 5; intento++) {
      entrar("agente@acme.test", "mala", "203.0.113.20").andExpect(status().isUnauthorized());
    }
    entrar("agente@acme.test", "demo-resolvo", "203.0.113.20")
        .andExpect(status().isTooManyRequests());

    entrar("agente@acme.test", "demo-resolvo", "203.0.113.21").andExpect(status().isOk());
  }

  @Test
  void elLimiteEsPorCuentaYPorClienteNoGlobal() throws Exception {
    for (int intento = 0; intento < 5; intento++) {
      entrar("agente@acme.test", "mala", "203.0.113.30").andExpect(status().isUnauthorized());
    }

    entrar("admin@acme.test", "demo-resolvo", "203.0.113.30").andExpect(status().isOk());
  }

  @Test
  void unLoginCorrectoReiniciaLaCuenta() throws Exception {
    for (int intento = 0; intento < 4; intento++) {
      entrar("agente@acme.test", "mala", "203.0.113.40").andExpect(status().isUnauthorized());
    }
    entrar("agente@acme.test", "demo-resolvo", "203.0.113.40").andExpect(status().isOk());

    for (int intento = 0; intento < 4; intento++) {
      entrar("agente@acme.test", "mala", "203.0.113.40").andExpect(status().isUnauthorized());
    }
  }

  @Test
  void laOnceavaSugerenciaDelMinutoDaUn429() throws Exception {
    String agente = HttpAyuda.organizacionNueva(mvc, jdbc).agente();

    for (int intento = 0; intento < 10; intento++) {
      mvc.perform(
              post("/api/tickets/%s/sugerencias".formatted(UUID.randomUUID()))
                  .header("Authorization", "Bearer " + agente))
          .andExpect(status().isNotFound());
    }

    var respuesta =
        mvc.perform(
                post("/api/tickets/%s/sugerencias".formatted(UUID.randomUUID()))
                    .header("Authorization", "Bearer " + agente))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").value("demasiadas-peticiones"))
            .andReturn()
            .getResponse();
    assertThat(Long.parseLong(respuesta.getHeader("Retry-After"))).isBetween(1L, 60L);
  }

  @Test
  void elLimiteDeIaEsPorUsuarioYCuentaTambienLasClasificaciones() throws Exception {
    String uno = HttpAyuda.organizacionNueva(mvc, jdbc).agente();
    String otro = HttpAyuda.organizacionNueva(mvc, jdbc).agente();
    for (int intento = 0; intento < 10; intento++) {
      mvc.perform(
              post("/api/tickets/%s/clasificaciones".formatted(UUID.randomUUID()))
                  .header("Authorization", "Bearer " + uno))
          .andExpect(status().isNotFound());
    }

    mvc.perform(
            post("/api/tickets/%s/sugerencias".formatted(UUID.randomUUID()))
                .header("Authorization", "Bearer " + uno))
        .andExpect(status().isTooManyRequests());
    mvc.perform(
            post("/api/tickets/%s/sugerencias".formatted(UUID.randomUUID()))
                .header("Authorization", "Bearer " + otro))
        .andExpect(status().isNotFound());
  }

  @Test
  void lasLecturasDeIaNoCuentanParaElLimite() throws Exception {
    String agente = HttpAyuda.organizacionNueva(mvc, jdbc).agente();

    for (int intento = 0; intento < 12; intento++) {
      mvc.perform(
              get("/api/tickets/%s/sugerencias/actual".formatted(UUID.randomUUID()))
                  .header("Authorization", "Bearer " + agente))
          .andExpect(status().isNotFound());
    }
    mvc.perform(
            post("/api/tickets/%s/sugerencias".formatted(UUID.randomUUID()))
                .header("Authorization", "Bearer " + agente))
        .andExpect(status().isNotFound());
  }

  private ResultActions entrarFirmado(String email, String ip, String secreto) throws Exception {
    return mvc.perform(
        post("/api/sesion")
            .header("CF-Connecting-IP", "2a06:98c0:3600::103")
            .header("X-Resolvo-Proxy", secreto)
            .header("X-Cliente-Ip", ip)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"%s\",\"contrasena\":\"mala\"}".formatted(email)));
  }

  @Test
  void detrasDelProxyFirmadoCadaClienteTieneSuPropioLimite() throws Exception {
    for (int intento = 0; intento < 5; intento++) {
      entrarFirmado("agente@acme.test", "203.0.113.50", "secreto-de-prueba")
          .andExpect(status().isUnauthorized());
    }
    entrarFirmado("agente@acme.test", "203.0.113.50", "secreto-de-prueba")
        .andExpect(status().isTooManyRequests());

    entrarFirmado("agente@acme.test", "203.0.113.51", "secreto-de-prueba")
        .andExpect(status().isUnauthorized());
  }

  @Test
  void conUnSecretoFalsoLaIpAjenaNoSirveParaEvitarElLimite() throws Exception {
    for (int intento = 0; intento < 5; intento++) {
      entrarFirmado("agente@acme.test", "203.0.113.60", "falso").andExpect(status().isUnauthorized());
    }

    // Cambiar X-Cliente-Ip sin el secreto correcto no da otro cupo: la clave sigue siendo la misma.
    entrarFirmado("agente@acme.test", "203.0.113.61", "falso")
        .andExpect(status().isTooManyRequests());
  }
}
