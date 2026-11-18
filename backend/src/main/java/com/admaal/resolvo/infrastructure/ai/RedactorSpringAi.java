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
      PlantillaDePrompt.cargar("prompts/redactar-respuesta-v1.txt");

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
    return interpretar(llamador.llamar(prompt(ticket, fragmentos)).texto());
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
    return new BorradorConCitas(texto.asText(), citas);
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
          .append("[id: ")
          .append(fragmento.id().valor())
          .append(" | ")
          .append(fragmento.titulo())
          .append("]\n")
          .append(fragmento.texto())
          .append("\n\n");
    }
    return PLANTILLA.rellenar(
        Map.of(
            "fragmentos", bloque.toString().strip(),
            "asunto", PlantillaDePrompt.sinDelimitadores(ticket.asunto()),
            "cuerpo", PlantillaDePrompt.sinDelimitadores(ticket.cuerpo())));
  }
}
