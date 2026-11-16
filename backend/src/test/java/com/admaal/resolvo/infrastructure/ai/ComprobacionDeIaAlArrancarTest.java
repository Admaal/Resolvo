package com.admaal.resolvo.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class ComprobacionDeIaAlArrancarTest {

  private final ChatModel chat = mock(ChatModel.class);
  private final RedactorSpringAi redactor =
      new RedactorSpringAi(chat, new ObjectMapper(), List.of("a", "b"), Duration.ofSeconds(5));

  @Test
  void conIaDisponibleSondeaCadaModeloConUnaLlamadaMinimaYNoFalla() {
    AtomicInteger llamadas = new AtomicInteger();
    when(chat.call(any(Prompt.class)))
        .thenAnswer(
            invocacion -> {
              llamadas.incrementAndGet();
              return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
            });

    new ComprobacionDeIaAlArrancar(redactor, Runnable::run, true).comprobar();

    assertThat(llamadas.get()).isEqualTo(2);
    assertThat(redactor.modelo()).isIn("a", "b");
  }

  @Test
  void siLaIaNoRespondeNoTumbaLaAplicacion() {
    when(chat.call(any(Prompt.class))).thenThrow(new RuntimeException("503 caido"));

    assertThatCode(() -> new ComprobacionDeIaAlArrancar(redactor, Runnable::run, true).comprobar())
        .doesNotThrowAnyException();
  }

  @Test
  void alArrancarSoloComprueba_siEstaHabilitada() {
    AtomicInteger tareas = new AtomicInteger();

    new ComprobacionDeIaAlArrancar(redactor, tarea -> tareas.incrementAndGet(), false).alArrancar();
    new ComprobacionDeIaAlArrancar(redactor, tarea -> tareas.incrementAndGet(), true).alArrancar();

    assertThat(tareas.get()).isEqualTo(1);
  }
}
