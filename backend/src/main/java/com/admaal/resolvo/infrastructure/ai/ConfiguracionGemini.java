package com.admaal.resolvo.infrastructure.ai;

import com.admaal.resolvo.application.ClasificadorTickets;
import com.admaal.resolvo.application.Embebedor;
import com.admaal.resolvo.application.RegistroLlamadas;
import com.admaal.resolvo.application.Reloj;
import com.admaal.resolvo.domain.LlamadaIa;
import com.admaal.resolvo.domain.OrganizacionId;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("gemini")
public class ConfiguracionGemini {

  /**
   * Falla antes de crear ningun bean: sin clave el SDK de Google fallaria despues con un error
   * opaco. Una clave presente pero incorrecta no tumba la API: las llamadas dan 503.
   */
  @Bean
  static BeanFactoryPostProcessor exigirClave(Environment entorno) {
    return fabrica -> {
      String clave = entorno.getProperty("spring.ai.google.genai.api-key");
      if (clave == null || clave.isBlank()) {
        throw new IllegalStateException(
            "El perfil gemini necesita la variable RESOLVO_GEMINI_API_KEY. "
                + "Sin clave, quita el perfil gemini y la IA queda apagada.");
      }
    };
  }

  @Bean
  Embebedor embebedor(
      EmbeddingModel modelo,
      @Value("${spring.ai.google.genai.embedding.text.options.model}") String nombre) {
    return new EmbebedorSpringAi(modelo, nombre);
  }

  /** Un solo llamador: el sondeo de modelos al arrancar sirve al redactor y al clasificador. */
  @Bean
  LlamadorGemini llamadorGemini(
      ChatModel chat,
      @Value("${resolvo.gemini.modelos}") List<String> modelos,
      @Value("${resolvo.gemini.timeout-segundos:15}") int segundos,
      @Value("${resolvo.gemini.max-intentos:3}") int maxIntentos,
      RegistroLlamadas registro,
      Reloj reloj) {
    LlamadorGemini llamador =
        new LlamadorGemini(chat, modelos, Duration.ofSeconds(segundos), maxIntentos);
    llamador.observarLlamadas(
        observacion ->
            registro.registrar(
                new LlamadaIa(
                    UUID.randomUUID(),
                    observacion.contexto().organizacionId() == null
                        ? null
                        : new OrganizacionId(observacion.contexto().organizacionId()),
                    observacion.contexto().operacion(),
                    observacion.modelo(),
                    observacion.tokensEntrada(),
                    observacion.tokensSalida(),
                    observacion.milisegundos(),
                    observacion.exito(),
                    reloj.ahora())));
    return llamador;
  }

  /** Devuelve el tipo concreto para que la comprobacion de arranque pueda inyectarlo. */
  @Bean
  RedactorSpringAi redactor(LlamadorGemini llamador, ObjectMapper json) {
    return new RedactorSpringAi(llamador, json);
  }

  @Bean
  ClasificadorTickets clasificador(LlamadorGemini llamador, ObjectMapper json) {
    return new ClasificadorSpringAi(llamador, json);
  }
}
