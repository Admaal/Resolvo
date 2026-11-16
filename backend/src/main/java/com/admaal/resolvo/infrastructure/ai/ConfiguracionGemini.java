package com.admaal.resolvo.infrastructure.ai;

import com.admaal.resolvo.application.Embebedor;
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

  /** Devuelve el tipo concreto para que la comprobacion de arranque pueda inyectarlo. */
  @Bean
  RedactorSpringAi redactor(
      ChatModel chat,
      ObjectMapper json,
      @Value("${resolvo.gemini.modelos}") List<String> modelos,
      @Value("${resolvo.gemini.timeout-segundos:20}") int segundos) {
    return new RedactorSpringAi(chat, json, modelos, Duration.ofSeconds(segundos));
  }
}
