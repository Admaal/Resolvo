package com.admaal.resolvo.infrastructure.ai;

import com.admaal.resolvo.application.Embebedor;
import com.admaal.resolvo.application.RedactorRespuestas;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("gemini")
public class ConfiguracionGemini {

  @Bean
  Embebedor embebedor(
      EmbeddingModel modelo,
      @Value("${spring.ai.google.genai.embedding.text.options.model}") String nombre) {
    return new EmbebedorSpringAi(modelo, nombre);
  }

  @Bean
  RedactorRespuestas redactor(
      ChatModel chat,
      ObjectMapper json,
      @Value("${spring.ai.google.genai.chat.options.model}") String nombre) {
    return new RedactorSpringAi(chat, json, nombre);
  }
}
