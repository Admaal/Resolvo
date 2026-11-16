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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.ClassPathResource;

public class RedactorSpringAi implements RedactorRespuestas {

  private static final Pattern DELIMITADOR =
      Pattern.compile("<\s*/?\s*ticket\s*>", Pattern.CASE_INSENSITIVE);
  private static final Pattern MARCADOR = Pattern.compile("\\{\\{(\\w+)}}");
  private static final String PLANTILLA = cargar("prompts/redactar-respuesta-v1.txt");

  private final ChatModel chat;
  private final ObjectMapper json;
  private final String nombreDelModelo;

  public RedactorSpringAi(ChatModel chat, ObjectMapper json, String nombreDelModelo) {
    this.chat = chat;
    this.json = json;
    this.nombreDelModelo = nombreDelModelo;
  }

  @Override
  public String modelo() {
    return nombreDelModelo;
  }

  @Override
  public BorradorConCitas redactar(Ticket ticket, List<Fragmento> fragmentos) {
    String respuesta;
    try {
      respuesta = chat.call(prompt(ticket, fragmentos));
    } catch (RuntimeException error) {
      throw FalloDelProveedor.de(error);
    }
    return interpretar(respuesta);
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
    Map<String, String> valores =
        Map.of(
            "fragmentos", bloque.toString().strip(),
            "asunto", sinDelimitadores(ticket.asunto()),
            "cuerpo", sinDelimitadores(ticket.cuerpo()));
    // Una sola pasada: el texto insertado no vuelve a pasar por el reemplazo de marcadores.
    Matcher marcador = MARCADOR.matcher(PLANTILLA);
    StringBuilder prompt = new StringBuilder();
    while (marcador.find()) {
      marcador.appendReplacement(
          prompt, Matcher.quoteReplacement(valores.getOrDefault(marcador.group(1), marcador.group())));
    }
    marcador.appendTail(prompt);
    return prompt.toString();
  }

  private static String sinDelimitadores(String texto) {
    return DELIMITADOR.matcher(texto).replaceAll("");
  }

  private static String cargar(String ruta) {
    try (var entrada = new ClassPathResource(ruta).getInputStream()) {
      return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException error) {
      throw new UncheckedIOException(error);
    }
  }
}
