package com.admaal.resolvo.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
@AutoConfigureMockMvc
class SesionHttpTest {

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired MockMvc mvc;

  @Test
  void elRefreshViajaEnCookieHttpOnly() throws Exception {
    MvcResult resultado =
        mvc.perform(
                post("/api/sesion")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"cliente@acme.test\",\"contrasena\":\"demo-resolvo\"}"))
            .andExpect(status().isOk())
            .andReturn();

    String cookie = resultado.getResponse().getHeader("Set-Cookie");
    assertThat(cookie).contains("HttpOnly");
    assertThat(cookie).contains("SameSite=Lax");
    assertThat(cookie).contains("Path=/api/sesion");
    assertThat(cookie).doesNotContain("Secure");
    String valor = cookie.split(";", 2)[0].substring("refresh=".length());
    assertThat(resultado.getResponse().getContentAsString()).doesNotContain(valor);
  }
}
