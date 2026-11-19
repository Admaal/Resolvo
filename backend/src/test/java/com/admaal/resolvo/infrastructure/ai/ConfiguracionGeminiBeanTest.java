package com.admaal.resolvo.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.admaal.resolvo.application.soporte.RegistroLlamadasEnMemoria;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/** El bean de produccion deja el observador de metricas conectado, no solo el metodo suelto. */
class ConfiguracionGeminiBeanTest {

  @Test
  void elBeanDelLlamadorRegistraCadaPeticionEnElRegistroDeLlamadas() {
    ChatModel chat = mock(ChatModel.class);
    when(chat.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("hola")))));
    RegistroLlamadasEnMemoria registro = new RegistroLlamadasEnMemoria();
    Instant ahora = Instant.parse("2026-11-19T12:00:00Z");

    LlamadorGemini llamador =
        new ConfiguracionGemini().llamadorGemini(chat, List.of("m1"), 5, 1, registro, () -> ahora);
    llamador.llamar(new LlamadorGemini.Contexto("sugerir", UUID.randomUUID()), "di hola");

    assertThat(registro.todas()).singleElement().satisfies(fila -> {
      assertThat(fila.operacion()).isEqualTo("sugerir");
      assertThat(fila.ocurridaEn()).isEqualTo(ahora);
    });
  }
}
