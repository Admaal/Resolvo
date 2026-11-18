package com.admaal.resolvo.infrastructure.web;

import static com.admaal.resolvo.infrastructure.web.HttpAyuda.INCIDENCIA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Sin perfil gemini ni clave: clasificar responde 503 con un motivo claro y el ticket no cambia. */
@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
@AutoConfigureMockMvc
class ClasificacionSinIaHttpTest {

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired MockMvc mvc;

  @Test
  void sinClaveClasificarResponde503ConElMotivoYElTicketSigueIgual() throws Exception {
    String agente = HttpAyuda.token(mvc, "agente@acme.test");
    String ticket = HttpAyuda.crearTicket(mvc, agente, INCIDENCIA, "Sin clave");

    mvc.perform(
            post("/api/tickets/" + ticket + "/clasificaciones").header("Authorization", "Bearer " + agente))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("ia-no-configurada"));

    mvc.perform(get("/api/tickets/" + ticket).header("Authorization", "Bearer " + agente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.prioridad").value("ALTA"));
  }
}
