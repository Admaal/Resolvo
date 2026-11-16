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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.core.io.ClassPathResource;

public class RedactorSpringAi implements RedactorRespuestas {

  private static final Logger LOG = LoggerFactory.getLogger(RedactorSpringAi.class);
  private static final Pattern DELIMITADOR =
      Pattern.compile("<\\s*/?\\s*ticket\\s*>", Pattern.CASE_INSENSITIVE);
  private static final Pattern MARCADOR = Pattern.compile("\\{\\{(\\w+)}}");
  private static final String PLANTILLA = cargar("prompts/redactar-respuesta-v1.txt");

  private final ChatModel chat;
  private final ObjectMapper json;
  private final List<String> modelos;
  private final Duration limite;
  private final ExecutorService llamadas =
      Executors.newCachedThreadPool(
          tarea -> {
            Thread hilo = new Thread(tarea, "gemini-llamada");
            hilo.setDaemon(true);
            return hilo;
          });
  private final int maxIntentos;
  private volatile List<String> orden;
  private volatile String modeloUsado;

  private record Sondeo(String modelo, boolean responde, long milisegundos, IaNoDisponible fallo) {}

  public RedactorSpringAi(
      ChatModel chat, ObjectMapper json, List<String> modelos, Duration limitePorIntento) {
    this(chat, json, modelos, limitePorIntento, modelos.size());
  }

  /**
   * Los modelos se prueban por orden: Google retira o satura modelos concretos (404, 503, cuota) y
   * una demo no puede depender de que uno siga vivo. Cada intento tiene un limite de tiempo y una
   * peticion no prueba mas de {@code maxIntentos} modelos, para acotar cuanto puede tardar.
   */
  public RedactorSpringAi(
      ChatModel chat,
      ObjectMapper json,
      List<String> modelos,
      Duration limitePorIntento,
      int maxIntentos) {
    if (modelos.isEmpty()) {
      throw new IllegalArgumentException("hace falta al menos un modelo");
    }
    this.chat = chat;
    this.json = json;
    this.modelos = List.copyOf(modelos);
    this.orden = this.modelos;
    this.limite = limitePorIntento;
    this.maxIntentos = Math.max(1, maxIntentos);
    this.modeloUsado = this.modelos.get(0);
  }

  /** El modelo que contesto en la ultima llamada; antes de la primera, el preferido. */
  @Override
  public String modelo() {
    return modeloUsado;
  }

  /**
   * Sondea todos los modelos a la vez con una llamada minima y sin datos de nadie, deja en el log
   * cuanto tarda cada uno y pone primero los que responden, del mas rapido al mas lento. Los que
   * fallan quedan al final como ultimo recurso: pueden recuperarse.
   */
  public String comprobar() {
    List<Future<Sondeo>> tareas = new ArrayList<>();
    for (String modelo : modelos) {
      tareas.add(llamadas.submit(() -> sondear(modelo)));
    }
    List<Sondeo> sanos = new ArrayList<>();
    IaNoDisponible ultimoFallo = null;
    for (Future<Sondeo> tarea : tareas) {
      Sondeo sondeo = esperar(tarea);
      if (sondeo.responde()) {
        sanos.add(sondeo);
      } else {
        ultimoFallo = sondeo.fallo();
      }
    }
    if (sanos.isEmpty()) {
      LOG.warn("Gemini sondeo: ningun modelo responde ({})", modelos);
      throw ultimoFallo;
    }
    sanos.sort(java.util.Comparator.comparingLong(Sondeo::milisegundos));
    List<String> nuevoOrden = new ArrayList<>();
    sanos.forEach(sondeo -> nuevoOrden.add(sondeo.modelo()));
    modelos.stream().filter(modelo -> !nuevoOrden.contains(modelo)).forEach(nuevoOrden::add);
    orden = List.copyOf(nuevoOrden);
    modeloUsado = orden.get(0);
    LOG.info("Gemini sondeo: orden de modelos {}", orden);
    return modeloUsado;
  }

  private Sondeo sondear(String modelo) {
    long inicio = System.nanoTime();
    try {
      llamar(modelo, "Responde solo con la palabra: ok");
      long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
      LOG.info("Gemini sondeo: {} responde en {} ms", modelo, ms);
      return new Sondeo(modelo, true, ms, null);
    } catch (RuntimeException error) {
      long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
      LOG.warn(
          "Gemini sondeo: {} no responde tras {} ms ({})",
          modelo,
          ms,
          FalloDelProveedor.resumen(error));
      return new Sondeo(modelo, false, ms, FalloDelProveedor.de(error));
    }
  }

  private static Sondeo esperar(Future<Sondeo> tarea) {
    try {
      return tarea.get();
    } catch (InterruptedException interrumpido) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(interrumpido);
    } catch (ExecutionException fallo) {
      throw new IllegalStateException(fallo.getCause());
    }
  }

  @Override
  public BorradorConCitas redactar(Ticket ticket, List<Fragmento> fragmentos) {
    return interpretar(llamarConRespaldo(prompt(ticket, fragmentos)));
  }

  private String llamarConRespaldo(String prompt) {
    IaNoDisponible ultimo = null;
    int intentos = 0;
    for (String modelo : orden) {
      if (intentos++ >= maxIntentos) {
        break;
      }
      try {
        String texto = llamar(modelo, prompt);
        modeloUsado = modelo;
        return texto;
      } catch (RuntimeException error) {
        ultimo = FalloDelProveedor.de(error);
        if (!FalloDelProveedor.sePuedeProbarOtroModelo(error)) {
          LOG.warn(
              "Gemini: el modelo {} falla sin remedio: {}", modelo, FalloDelProveedor.resumen(error));
          throw ultimo;
        }
        LOG.warn(
            "Gemini: el modelo {} no sirve ({}); pruebo el siguiente",
            modelo,
            FalloDelProveedor.resumen(error));
      }
    }
    LOG.warn("Gemini: ningun modelo respondio ({})", orden);
    throw ultimo;
  }

  private String llamar(String modelo, String prompt) {
    Future<ChatResponse> tarea =
        llamadas.submit(
            () ->
                chat.call(
                    new Prompt(prompt, GoogleGenAiChatOptions.builder().model(modelo).build())));
    ChatResponse respuesta;
    try {
      respuesta = tarea.get(limite.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException agotado) {
      tarea.cancel(true);
      throw new RuntimeException("tiempo agotado con el modelo " + modelo, agotado);
    } catch (InterruptedException interrumpido) {
      Thread.currentThread().interrupt();
      tarea.cancel(true);
      throw new IllegalStateException(interrumpido);
    } catch (ExecutionException fallo) {
      if (fallo.getCause() instanceof RuntimeException causa) {
        throw causa;
      }
      throw new RuntimeException(fallo.getCause());
    }
    if (respuesta == null || respuesta.getResult() == null) {
      throw new IaNoDisponible("ia-no-disponible");
    }
    return respuesta.getResult().getOutput().getText();
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
    // Se repite hasta que no cambie: quitar uno puede juntar los trozos de otro ("<tic<ticket>ket>").
    String actual = texto;
    String anterior;
    do {
      anterior = actual;
      actual = DELIMITADOR.matcher(anterior).replaceAll("");
    } while (!actual.equals(anterior));
    return actual;
  }

  private static String cargar(String ruta) {
    try (var entrada = new ClassPathResource(ruta).getInputStream()) {
      return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException error) {
      throw new UncheckedIOException(error);
    }
  }
}
