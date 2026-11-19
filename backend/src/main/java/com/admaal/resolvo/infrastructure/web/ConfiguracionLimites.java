package com.admaal.resolvo.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionLimites implements WebMvcConfigurer {

  private final LimiteDeIa limiteDeIa;

  public ConfiguracionLimites(LimiteDeIa limiteDeIa) {
    this.limiteDeIa = limiteDeIa;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registro) {
    registro
        .addInterceptor(limiteDeIa)
        .addPathPatterns(
            "/api/tickets/*/sugerencias",
            "/api/tickets/*/clasificaciones",
            "/api/conocimiento/reindexar");
  }
}
