package com.admaal.resolvo.infrastructure.ai;

import com.admaal.resolvo.application.BorradorConCitas;
import com.admaal.resolvo.application.IaNoDisponible;
import com.admaal.resolvo.application.RedactorRespuestas;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.FragmentoId;
import com.admaal.resolvo.domain.Ticket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.model.ChatModel;

public class RedactorSpringAi implements RedactorRespuestas {

  private static final PlantillaDePrompt PLANTILLA =
      PlantillaDePrompt.cargar("prompts/redactar-respuesta-v2.txt");

  /** Tope del borrador que se acepta del modelo; la columna admite 8000. */
  private static final int MAXIMO_DEL_BORRADOR = 4000;

  private final LlamadorGemini llamador;
  private final ObjectMapper json;

  public RedactorSpringAi(
      ChatModel chat, ObjectMapper json, List<String> modelos, Duration limitePorIntento) {
    this(chat, json, modelos, limitePorIntento, modelos.size());
  }

  public RedactorSpringAi(
      ChatModel chat,
      ObjectMapper json,
      List<String> modelos,
      Duration limitePorIntento,
      int maxIntentos) {
    this(new LlamadorGemini(chat, modelos, limitePorIntento, maxIntentos), json);
  }

  public RedactorSpringAi(LlamadorGemini llamador, ObjectMapper json) {
    this.llamador = llamador;
    this.json = json;
  }

  /** El modelo que contesto en la ultima llamada; antes de la primera, el preferido. */
  @Override
  public String modelo() {
    return llamador.modelo();
  }

  /** Sondea los modelos al arrancar; ver {@link LlamadorGemini#comprobar()}. */
  public String comprobar() {
    return llamador.comprobar();
  }

  @Override
  public BorradorConCitas redactar(Ticket ticket, List<Fragmento> fragmentos) {
    LlamadorGemini.Contexto contexto =
        new LlamadorGemini.Contexto("sugerir", ticket.organizacionId().valor());
    return interpretar(llamador.llamar(contexto, prompt(ticket, fragmentos)).texto());
  }

  private BorradorConCitas interpretar(String respuesta) {
    JsonNode raiz = leer(respuesta);
    JsonNode texto = raiz.get("respuesta");
    if (texto == null || !texto.isTextual() || texto.asText().isBlank()) {
      throw new IaNoDisponible("ia-no-disponible");
    }
    List<FragmentoId> citas = new ArrayList<>();
    JsonNode lista = raiz.get("citas");
    if (lista != null && lista.isArray()) {
      for (JsonNode cita : lista) {
        try {
          citas.add(new FragmentoId(UUID.fromString(cita.asText())));
        } catch (IllegalArgumentException ignorada) {
          // Un id que no es un UUID no puede ser un fragmento recuperado: se descarta.
        }
      }
    }
    String borrador = texto.asText();
    if (borrador.length() > MAXIMO_DEL_BORRADOR) {
      borrador = borrador.substring(0, MAXIMO_DEL_BORRADOR);
    }
    return new BorradorConCitas(borrador, citas);
  }

  private JsonNode leer(String respuesta) {
    if (respuesta == null) {
      throw new IaNoDisponible("ia-no-disponible");
    }
    int inicio = respuesta.indexOf('{');
    int fin = respuesta.lastIndexOf('}');
    if (inicio < 0 || fin < inicio) {
      throw new IaNoDisponible("ia-no-disponible");
    }
    try {
      return json.readTree(respuesta.substring(inicio, fin + 1));
    } catch (IOException error) {
      throw new IaNoDisponible("ia-no-disponible", error);
    }
  }

  private static String prompt(Ticket ticket, List<Fragmento> fragmentos) {
    StringBuilder bloque = new StringBuilder();
    for (Fragmento fragmento : fragmentos) {
      bloque
          .append("<fuente id=\"")
          .append(fragmento.id().valor())
          .append("\">\nTitulo: ")
          .append(PlantillaDePrompt.limpiar(fragmento.titulo()))
          .append('\n')
          .append(PlantillaDePrompt.limpiar(fragmento.texto()))
          .append("\n</fuente>\n\n");
    }
    return PLANTILLA.rellenar(
        Map.of(
            "fragmentos", bloque.toString().strip(),
            "asunto", PlantillaDePrompt.limpiar(ticket.asunto()),
            "cuerpo", PlantillaDePrompt.limpiar(ticket.cuerpo())));
  }
}
