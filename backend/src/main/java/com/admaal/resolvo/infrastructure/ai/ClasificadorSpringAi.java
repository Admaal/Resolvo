package com.admaal.resolvo.infrastructure.ai;

import com.admaal.resolvo.application.ClasificacionPropuesta;
import com.admaal.resolvo.application.ClasificadorTickets;
import com.admaal.resolvo.application.IaNoDisponible;
import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Ticket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClasificadorSpringAi implements ClasificadorTickets {

  /** Igual que el limite del dominio: un motivo mas largo se recorta, no se rechaza. */
  private static final int MAXIMO_MOTIVO = 500;

  private static final PlantillaDePrompt PLANTILLA =
      PlantillaDePrompt.cargar("prompts/clasificar-ticket-v2.txt");

  private final LlamadorGemini llamador;
  private final ObjectMapper json;

  public ClasificadorSpringAi(LlamadorGemini llamador, ObjectMapper json) {
    this.llamador = llamador;
    this.json = json;
  }

  @Override
  public String modelo() {
    return llamador.modelo();
  }

  @Override
  public ClasificacionPropuesta clasificar(Ticket ticket, List<Categoria> categorias) {
    LlamadorGemini.Contexto contexto =
        new LlamadorGemini.Contexto("clasificar", ticket.organizacionId().valor());
    return interpretar(llamador.llamar(contexto, prompt(ticket, categorias)).texto());
  }

  private ClasificacionPropuesta interpretar(String respuesta) {
    JsonNode raiz = leer(respuesta);
    return new ClasificacionPropuesta(
        texto(raiz, "categoria"), prioridad(texto(raiz, "prioridad")), motivo(texto(raiz, "motivo")));
  }

  private static Prioridad prioridad(String valor) {
    if (valor == null) {
      throw new IaNoDisponible("clasificacion-invalida");
    }
    try {
      return Prioridad.valueOf(valor.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException desconocida) {
      throw new IaNoDisponible("clasificacion-invalida", desconocida);
    }
  }

  private static String motivo(String valor) {
    if (valor == null) {
      return null;
    }
    String limpio = valor.strip();
    return limpio.length() > MAXIMO_MOTIVO ? limpio.substring(0, MAXIMO_MOTIVO) : limpio;
  }

  private static String texto(JsonNode raiz, String campo) {
    JsonNode nodo = raiz.get(campo);
    return nodo != null && nodo.isTextual() ? nodo.asText() : null;
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

  private static String prompt(Ticket ticket, List<Categoria> categorias) {
    StringBuilder lista = new StringBuilder();
    for (Categoria categoria : categorias) {
      lista.append("- ").append(PlantillaDePrompt.sinDelimitadores(categoria.nombre())).append('\n');
    }
    return PLANTILLA.rellenar(
        Map.of(
            "categorias", lista.toString().strip(),
            "asunto", PlantillaDePrompt.sinDelimitadores(ticket.asunto()),
            "cuerpo", PlantillaDePrompt.sinDelimitadores(ticket.cuerpo())));
  }
}
