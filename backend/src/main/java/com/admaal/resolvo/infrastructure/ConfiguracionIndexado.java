package com.admaal.resolvo.infrastructure;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionIndexado {

  /** Un solo hilo: el indexado va en segundo plano y no compite con las peticiones. En tests, sincrono. */
  @Bean(name = "indexadorEjecutor")
  Executor indexadorEjecutor(@Value("${resolvo.ia.sincrona:false}") boolean sincrona) {
    if (sincrona) {
      return Runnable::run;
    }
    return Executors.newSingleThreadExecutor(
        tarea -> {
          Thread hilo = new Thread(tarea, "indexador");
          hilo.setDaemon(true);
          return hilo;
        });
  }
}
