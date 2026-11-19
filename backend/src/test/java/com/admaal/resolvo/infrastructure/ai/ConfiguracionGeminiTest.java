package com.admaal.resolvo.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.admaal.resolvo.application.IaNoDisponible;
import com.admaal.resolvo.application.RegistroLlamadas;
import com.admaal.resolvo.application.soporte.RegistroLlamadasEnMemoria;
import com.admaal.resolvo.domain.Consumo;
import com.admaal.resolvo.domain.LlamadaIa;
import com.admaal.resolvo.domain.OrganizacionId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/** El observador que convierte cada peticion a Gemini en una fila de metricas. */
class ConfiguracionGeminiTest {

  private static final Instant AHORA = Instant.parse("2026-11-19T12:00:00Z");

  private final ChatModel chat = mock(ChatModel.class);
  private final LlamadorGemini llamador =
      new LlamadorGemini(chat, List.of("m1"), Duration.ofSeconds(5), 1);
  private final RegistroLlamadasEnMemoria registro = new RegistroLlamadasEnMemoria();
  private final UUID org = UUID.randomUUID();

  private void responde(String texto, int entrada, int salida) {
    when(chat.call(any(Prompt.class)))
        .thenReturn(
            new ChatResponse(
                List.of(new Generation(new AssistantMessage(texto))),
                ChatResponseMetadata.builder().usage(new DefaultUsage(entrada, salida)).build()));
  }

  @Test
  void cadaPeticionDejaUnaFilaConSuOrganizacionOperacionYTokens() {
    responde("hola", 12, 7);
    ConfiguracionGemini.anotarLlamadas(llamador, registro, () -> AHORA);

    llamador.llamar(new LlamadorGemini.Contexto("sugerir", org), "di hola");

    assertThat(registro.todas())
        .singleElement()
        .satisfies(
            fila -> {
              assertThat(fila.organizacionId()).isEqualTo(new OrganizacionId(org));
              assertThat(fila.operacion()).isEqualTo("sugerir");
              assertThat(fila.modelo()).isEqualTo("m1");
              assertThat(fila.tokensEntrada()).isEqualTo(12);
              assertThat(fila.tokensSalida()).isEqualTo(7);
              assertThat(fila.exito()).isTrue();
              assertThat(fila.ocurridaEn()).isEqualTo(AHORA);
              assertThat(fila.id()).isNotNull();
            });
  }

  @Test
  void unaPeticionFallidaTambienDejaFilaYUnaSinContextoNoTieneOrganizacion() {
    when(chat.call(any(Prompt.class))).thenThrow(new RuntimeException("403 clave invalida"));
    ConfiguracionGemini.anotarLlamadas(llamador, registro, () -> AHORA);

    assertThatThrownBy(() -> llamador.llamar("sin contexto")).isInstanceOf(IaNoDisponible.class);

    assertThat(registro.todas())
        .singleElement()
        .satisfies(
            fila -> {
              assertThat(fila.exito()).isFalse();
              assertThat(fila.organizacionId()).isNull();
              assertThat(fila.operacion()).isEqualTo("sin-contexto");
              assertThat(fila.tokensEntrada()).isZero();
            });
  }

  @Test
  void siElRegistroFallaLaPeticionSigueFuncionando() {
    responde("hola", 1, 1);
    ConfiguracionGemini.anotarLlamadas(
        llamador,
        new RegistroLlamadas() {
          @Override
          public void registrar(LlamadaIa llamada) {
            throw new IllegalStateException("base caida");
          }

          @Override
          public List<LlamadaIa> deLaOrganizacion(OrganizacionId o, Instant d, Instant h) {
            return List.of();
          }

          @Override
          public Consumo consumoDe(OrganizacionId o, Instant d, Instant h) {
            return new Consumo(0, 0);
          }
        },
        () -> AHORA);

    assertThat(llamador.llamar("di hola").texto()).isEqualTo("hola");
  }
}
