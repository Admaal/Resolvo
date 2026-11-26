package com.admaal.resolvo.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.admaal.resolvo.application.IaNoDisponible;
import java.time.Duration;
import java.util.ArrayList;
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
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;

class LlamadorGeminiTest {

  private final ChatModel chat = mock(ChatModel.class);

  private LlamadorGemini llamador(String... modelos) {
    return new LlamadorGemini(chat, List.of(modelos), Duration.ofSeconds(5), modelos.length);
  }

  private static ChatResponse conUso(String texto, int entrada, int salida) {
    return new ChatResponse(
        List.of(new Generation(new AssistantMessage(texto))),
        ChatResponseMetadata.builder().usage(new DefaultUsage(entrada, salida)).build());
  }

  private static String modeloDe(Prompt prompt) {
    return ((GoogleGenAiChatOptions) prompt.getOptions()).getModel();
  }

  @Test
  void devuelveTokensYDuracionDeLaLlamada() {
    when(chat.call(any(Prompt.class))).thenReturn(conUso("hola", 12, 7));

    LlamadorGemini.Respuesta respuesta = llamador("modelo-x").llamar("di hola");

    assertThat(respuesta.texto()).isEqualTo("hola");
    assertThat(respuesta.modelo()).isEqualTo("modelo-x");
    assertThat(respuesta.tokensEntrada()).isEqualTo(12);
    assertThat(respuesta.tokensSalida()).isEqualTo(7);
    assertThat(respuesta.milisegundos()).isBetween(0L, 5_000L);
  }

  @Test
  void sinMetadatosDeUsoLosTokensSonCero() {
    when(chat.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("hola")))));

    LlamadorGemini.Respuesta respuesta = llamador("modelo-x").llamar("di hola");

    assertThat(respuesta.tokensEntrada()).isZero();
    assertThat(respuesta.tokensSalida()).isZero();
  }

  @Test
  void siElPrimerModeloNoExistePruebaElSiguienteYLoRecuerda() {
    LlamadorGemini llamador = llamador("retirado", "vivo");
    when(chat.call(any(Prompt.class)))
        .thenAnswer(
            llamada -> {
              if (modeloDe(llamada.getArgument(0)).equals("retirado")) {
                throw new RuntimeException("404 model no longer available");
              }
              return conUso("ok", 1, 1);
            });

    LlamadorGemini.Respuesta respuesta = llamador.llamar("hola");

    assertThat(respuesta.modelo()).isEqualTo("vivo");
    assertThat(llamador.modelo()).isEqualTo("vivo");
  }

  @Test
  void unFalloSinRemedioNoPruebaOtroModelo() {
    when(chat.call(any(Prompt.class))).thenThrow(new RuntimeException("403 clave invalida"));

    assertThatThrownBy(() -> llamador("a", "b").llamar("hola")).isInstanceOf(IaNoDisponible.class);
  }

  @Test
  void sinRespuestaDelModeloEsIaNoDisponible() {
    when(chat.call(any(Prompt.class))).thenReturn(null);

    assertThatThrownBy(() -> llamador("a").llamar("hola"))
        .isInstanceOf(IaNoDisponible.class)
        .hasMessage("ia-no-disponible");
  }

  @Test
  void avisaALosObservadoresDeCadaLlamadaQueSirvio() {
    when(chat.call(any(Prompt.class))).thenReturn(conUso("hola", 12, 7));
    LlamadorGemini llamador = llamador("modelo-x");
    List<LlamadorGemini.Respuesta> vistas = new ArrayList<>();
    llamador.observar(vistas::add);

    LlamadorGemini.Respuesta respuesta = llamador.llamar("uno");
    llamador.llamar("dos");

    assertThat(vistas).hasSize(2);
    assertThat(vistas.get(0)).isEqualTo(respuesta);
  }

  @Test
  void unaLlamadaQueFallaNoSeNotificaYUnObservadorQueFallaNoLaRompe() {
    when(chat.call(any(Prompt.class)))
        .thenThrow(new RuntimeException("403 clave invalida"))
        .thenReturn(conUso("hola", 1, 1));
    LlamadorGemini llamador = llamador("modelo-x");
    List<LlamadorGemini.Respuesta> vistas = new ArrayList<>();
    llamador.observar(vistas::add);
    llamador.observar(
        respuesta -> {
          throw new IllegalStateException("observador roto");
        });

    assertThatThrownBy(() -> llamador.llamar("uno")).isInstanceOf(IaNoDisponible.class);
    assertThat(vistas).isEmpty();

    assertThat(llamador.llamar("dos").texto()).isEqualTo("hola");
    assertThat(vistas).hasSize(1);
  }

  @Test
  void elSondeoDeArranqueNoCuentaComoLlamadaObservada() {
    when(chat.call(any(Prompt.class))).thenReturn(conUso("ok", 1, 1));
    LlamadorGemini llamador = llamador("modelo-x");
    List<LlamadorGemini.Respuesta> vistas = new ArrayList<>();
    llamador.observar(vistas::add);

    llamador.comprobar();

    assertThat(vistas).isEmpty();
  }

  @Test
  void unaRespuestaRepartidaEnVariasPartesSeLeeEntera() {
    // Spring AI da una Generation por parte: si el modelo parte el JSON, getResult() solo trae la
    // primera y la respuesta llegaba cortada.
    ChatResponse enTrozos =
        new ChatResponse(
            List.of(
                new Generation(new AssistantMessage("{\"")),
                new Generation(new AssistantMessage("respuesta\": \"Hola")),
                new Generation(new AssistantMessage(" mundo\"}"))));
    when(chat.call(any(Prompt.class))).thenReturn(enTrozos);

    LlamadorGemini.Respuesta respuesta = llamador("modelo-x").llamar("di hola");

    assertThat(respuesta.texto()).isEqualTo("{\"respuesta\": \"Hola mundo\"}");
  }

  @Test
  void laspartesSinTextoNoRompenLaRespuesta() {
    ChatResponse conHuecos =
        new ChatResponse(
            List.of(
                new Generation(new AssistantMessage(null)),
                new Generation(new AssistantMessage("hola")),
                new Generation(new AssistantMessage(null))));
    when(chat.call(any(Prompt.class))).thenReturn(conHuecos);

    assertThat(llamador("modelo-x").llamar("di hola").texto()).isEqualTo("hola");
  }

  private final UUID organizacion = UUID.randomUUID();

  @Test
  void avisaConLaOperacionYLaOrganizacionDeCadaLlamada() {
    when(chat.call(any(Prompt.class))).thenReturn(conUso("hola", 12, 7));
    LlamadorGemini llamador = llamador("modelo-x");
    List<LlamadorGemini.Observacion> vistas = new ArrayList<>();
    llamador.observarLlamadas(vistas::add);

    llamador.llamar(new LlamadorGemini.Contexto("sugerir", organizacion), "di hola");

    assertThat(vistas)
        .singleElement()
        .satisfies(
            vista -> {
              assertThat(vista.contexto().operacion()).isEqualTo("sugerir");
              assertThat(vista.contexto().organizacionId()).isEqualTo(organizacion);
              assertThat(vista.modelo()).isEqualTo("modelo-x");
              assertThat(vista.tokensEntrada()).isEqualTo(12);
              assertThat(vista.tokensSalida()).isEqualTo(7);
              assertThat(vista.exito()).isTrue();
              assertThat(vista.milisegundos()).isBetween(0L, 5_000L);
            });
  }

  @Test
  void unaLlamadaSinContextoSeAnotaComoSinContexto() {
    when(chat.call(any(Prompt.class))).thenReturn(conUso("hola", 1, 1));
    LlamadorGemini llamador = llamador("modelo-x");
    List<LlamadorGemini.Observacion> vistas = new ArrayList<>();
    llamador.observarLlamadas(vistas::add);

    llamador.llamar("di hola");

    assertThat(vistas.get(0).contexto()).isEqualTo(LlamadorGemini.Contexto.SIN_CONTEXTO);
    assertThat(vistas.get(0).contexto().organizacionId()).isNull();
  }

  @Test
  void avisaElFalloConSuDuracionYSinTokens() {
    when(chat.call(any(Prompt.class))).thenThrow(new RuntimeException("403 clave invalida"));
    LlamadorGemini llamador = llamador("modelo-x");
    List<LlamadorGemini.Observacion> vistas = new ArrayList<>();
    llamador.observarLlamadas(vistas::add);

    assertThatThrownBy(
            () -> llamador.llamar(new LlamadorGemini.Contexto("clasificar", organizacion), "x"))
        .isInstanceOf(IaNoDisponible.class);

    assertThat(vistas)
        .singleElement()
        .satisfies(
            vista -> {
              assertThat(vista.exito()).isFalse();
              assertThat(vista.tokensEntrada()).isZero();
              assertThat(vista.tokensSalida()).isZero();
              assertThat(vista.modelo()).isEqualTo("modelo-x");
              assertThat(vista.contexto().operacion()).isEqualTo("clasificar");
              assertThat(vista.milisegundos()).isGreaterThanOrEqualTo(0L);
            });
  }

  @Test
  void conModelosDeRespaldoUnaPeticionDejaUnaSolaObservacion() {
    LlamadorGemini llamador = llamador("retirado", "vivo");
    when(chat.call(any(Prompt.class)))
        .thenAnswer(
            llamada -> {
              if (modeloDe(llamada.getArgument(0)).equals("retirado")) {
                throw new RuntimeException("404 model no longer available");
              }
              return conUso("ok", 3, 4);
            });
    List<LlamadorGemini.Observacion> vistas = new ArrayList<>();
    llamador.observarLlamadas(vistas::add);

    llamador.llamar("hola");

    assertThat(vistas).singleElement().satisfies(v -> assertThat(v.modelo()).isEqualTo("vivo"));
  }

  @Test
  void siNingunModeloRespondeLaPeticionDejaUnSoloFalloConElUltimoModelo() {
    LlamadorGemini llamador = llamador("a", "b");
    when(chat.call(any(Prompt.class))).thenThrow(new RuntimeException("503 saturado"));
    List<LlamadorGemini.Observacion> vistas = new ArrayList<>();
    llamador.observarLlamadas(vistas::add);

    assertThatThrownBy(() -> llamador.llamar("hola")).isInstanceOf(IaNoDisponible.class);

    assertThat(vistas)
        .singleElement()
        .satisfies(
            v -> {
              assertThat(v.exito()).isFalse();
              assertThat(v.modelo()).isEqualTo("b");
            });
  }

  @Test
  void elSondeoNoSeObservaYUnObservadorRotoNoRompeLaLlamada() {
    when(chat.call(any(Prompt.class))).thenReturn(conUso("ok", 1, 1));
    LlamadorGemini llamador = llamador("modelo-x");
    List<LlamadorGemini.Observacion> vistas = new ArrayList<>();
    llamador.observarLlamadas(
        vista -> {
          throw new IllegalStateException("observador roto");
        });
    llamador.observarLlamadas(vistas::add);

    llamador.comprobar();
    assertThat(vistas).isEmpty();

    assertThat(llamador.llamar("hola").texto()).isEqualTo("ok");
    assertThat(vistas).hasSize(1);
  }

  @Test
  void pideAlModeloElNivelDeRazonamientoConfigurado() {
    when(chat.call(any(Prompt.class))).thenReturn(conUso("ok", 1, 1));
    LlamadorGemini llamador =
        new LlamadorGemini(chat, List.of("modelo-x"), Duration.ofSeconds(5), 1, "low");

    llamador.llamar("hola");

    org.mockito.ArgumentCaptor<Prompt> enviado = org.mockito.ArgumentCaptor.forClass(Prompt.class);
    org.mockito.Mockito.verify(chat).call(enviado.capture());
    assertThat(((GoogleGenAiChatOptions) enviado.getValue().getOptions()).getThinkingLevel())
        .isEqualTo(org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel.LOW);
  }

  @Test
  void sinNivelConfiguradoDejaElDelModelo() {
    when(chat.call(any(Prompt.class))).thenReturn(conUso("ok", 1, 1));
    LlamadorGemini llamador =
        new LlamadorGemini(chat, List.of("modelo-x"), Duration.ofSeconds(5), 1, " ");

    llamador.llamar("hola");

    org.mockito.ArgumentCaptor<Prompt> enviado = org.mockito.ArgumentCaptor.forClass(Prompt.class);
    org.mockito.Mockito.verify(chat).call(enviado.capture());
    assertThat(((GoogleGenAiChatOptions) enviado.getValue().getOptions()).getThinkingLevel())
        .isNull();
  }

  @Test
  void unNivelDeRazonamientoDesconocidoNoArranca() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new LlamadorGemini(chat, List.of("modelo-x"), Duration.ofSeconds(5), 1, "mucho"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mucho");
  }
}
