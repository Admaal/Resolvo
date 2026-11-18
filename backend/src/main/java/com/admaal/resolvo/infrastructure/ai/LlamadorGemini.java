package com.admaal.resolvo.infrastructure.ai;

import com.admaal.resolvo.application.IaNoDisponible;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;

/**
 * Llama a Gemini con una lista de modelos de respaldo. Los modelos se prueban por orden: Google
 * retira o satura modelos concretos (404, 503, cuota) y una demo no puede depender de que uno siga
 * vivo. Cada intento tiene un limite de tiempo y una peticion no prueba mas de {@code maxIntentos}
 * modelos, para acotar cuanto puede tardar. Lo comparten el redactor y el clasificador.
 */
public class LlamadorGemini {

  private static final Logger LOG = LoggerFactory.getLogger(LlamadorGemini.class);

  /** Lo que contesto el modelo y lo que costo: tokens y milisegundos de la llamada que sirvio. */
  public record Respuesta(
      String texto, String modelo, int tokensEntrada, int tokensSalida, long milisegundos) {}

  private record Sondeo(String modelo, boolean responde, long milisegundos, IaNoDisponible fallo) {}

  private record Llamada(String texto, int tokensEntrada, int tokensSalida) {}

  private final ChatModel chat;
  private final List<String> modelos;
  private final Duration limite;
  private final int maxIntentos;
  private final ExecutorService llamadas =
      Executors.newCachedThreadPool(
          tarea -> {
            Thread hilo = new Thread(tarea, "gemini-llamada");
            hilo.setDaemon(true);
            return hilo;
          });
  private final List<Consumer<Respuesta>> observadores = new CopyOnWriteArrayList<>();
  private volatile List<String> orden;
  private volatile String modeloUsado;

  public LlamadorGemini(
      ChatModel chat, List<String> modelos, Duration limitePorIntento, int maxIntentos) {
    if (modelos.isEmpty()) {
      throw new IllegalArgumentException("hace falta al menos un modelo");
    }
    this.chat = chat;
    this.modelos = List.copyOf(modelos);
    this.orden = this.modelos;
    this.limite = limitePorIntento;
    this.maxIntentos = Math.max(1, maxIntentos);
    this.modeloUsado = this.modelos.get(0);
  }

  /** El modelo que contesto en la ultima llamada; antes de la primera, el preferido. */
  public String modelo() {
    return modeloUsado;
  }

  /**
   * Avisa de cada llamada que sirvio un modelo, con sus tokens y su duracion. Lo usan los evals y,
   * mas adelante, las metricas de la fase 5. El sondeo de arranque no cuenta, y un observador que
   * falla no rompe la llamada.
   */
  public void observar(Consumer<Respuesta> observador) {
    observadores.add(observador);
  }

  public Respuesta llamar(String prompt) {
    IaNoDisponible ultimo = null;
    int intentos = 0;
    for (String modelo : orden) {
      if (intentos++ >= maxIntentos) {
        break;
      }
      long inicio = System.nanoTime();
      try {
        Llamada llamada = llamar(modelo, prompt);
        modeloUsado = modelo;
        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
        Respuesta respuesta =
            new Respuesta(
                llamada.texto(), modelo, llamada.tokensEntrada(), llamada.tokensSalida(), ms);
        avisar(respuesta);
        return respuesta;
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

  private void avisar(Respuesta respuesta) {
    for (Consumer<Respuesta> observador : observadores) {
      try {
        observador.accept(respuesta);
      } catch (RuntimeException error) {
        LOG.warn("Un observador de Gemini fallo: {}", error.getMessage());
      }
    }
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
    sanos.sort(Comparator.comparingLong(Sondeo::milisegundos));
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

  private Llamada llamar(String modelo, String prompt) {
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
    Usage uso = respuesta.getMetadata() == null ? null : respuesta.getMetadata().getUsage();
    return new Llamada(
        textoDe(respuesta), tokens(uso, true), tokens(uso, false));
  }

  /**
   * Spring AI devuelve una Generation por cada parte de la respuesta de Gemini, y getResult() es
   * solo la primera: un modelo que reparte el JSON en varias partes llegaba cortado y se tomaba por
   * una respuesta no interpretable. Se une el texto de todas las partes.
   */
  private static String textoDe(ChatResponse respuesta) {
    StringBuilder texto = new StringBuilder();
    for (Generation parte : respuesta.getResults()) {
      String trozo = parte.getOutput() == null ? null : parte.getOutput().getText();
      if (trozo != null) {
        texto.append(trozo);
      }
    }
    return texto.toString();
  }

  private static int tokens(Usage uso, boolean entrada) {
    if (uso == null) {
      return 0;
    }
    Integer valor = entrada ? uso.getPromptTokens() : uso.getCompletionTokens();
    return valor == null ? 0 : valor;
  }
}
