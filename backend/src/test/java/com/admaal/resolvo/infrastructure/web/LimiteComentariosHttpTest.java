package com.admaal.resolvo.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
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
@SpringBootTest(
    classes = ResolvoApplication.class,
    properties = "resolvo.limites.comentarios-por-minuto=10")
@AutoConfigureMockMvc
class LimiteComentariosHttpTest {

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  private ResultActions comentar(String token, String ticket, String cuerpo) throws Exception {
    return mvc.perform(
        post("/api/tickets/" + ticket + "/comentarios")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"cuerpo\":\"%s\",\"interno\":false}".formatted(cuerpo)));
  }

  @Test
  void elComentarioOnceEnUnMinutoDa429ConRetryAfter() throws Exception {
    var organizacion = HttpAyuda.organizacionNueva(mvc, jdbc);
    String ticket =
        HttpAyuda.crearTicket(mvc, organizacion.cliente(), organizacion.categoriaId(), "Rafaga");

    for (int comentario = 1; comentario <= 10; comentario++) {
      comentar(organizacion.cliente(), ticket, "spam " + comentario).andExpect(status().isOk());
    }

    var respuesta =
        comentar(organizacion.cliente(), ticket, "spam 11")
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").value("demasiadas-peticiones"))
            .andReturn()
            .getResponse();
    assertThat(Long.parseLong(respuesta.getHeader("Retry-After"))).isBetween(1L, 60L);
  }

  @Test
  void elLimiteEsPorUsuarioYNoAfectaAOtraPersona() throws Exception {
    var organizacion = HttpAyuda.organizacionNueva(mvc, jdbc);
    String ticket =
        HttpAyuda.crearTicket(mvc, organizacion.cliente(), organizacion.categoriaId(), "Por usuario");
    for (int comentario = 1; comentario <= 10; comentario++) {
      comentar(organizacion.cliente(), ticket, "spam " + comentario).andExpect(status().isOk());
    }
    comentar(organizacion.cliente(), ticket, "spam 11").andExpect(status().isTooManyRequests());

    comentar(organizacion.agente(), ticket, "respuesta del equipo").andExpect(status().isOk());
  }
}
