package com.admaal.resolvo.infrastructure.ai;

import com.admaal.resolvo.application.IaNoDisponible;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Al arrancar deja en el log si la IA responde y con que modelo. Una llamada minima y sin datos
 * de nadie, en segundo plano: si falla, la app sigue y el log dice por que.
 */
@Component
@Profile("gemini")
public class ComprobacionDeIaAlArrancar {

  private static final Logger LOG = LoggerFactory.getLogger(ComprobacionDeIaAlArrancar.class);

  private final RedactorSpringAi redactor;
  private final Executor ejecutor;
  private final boolean habilitada;

  public ComprobacionDeIaAlArrancar(
      RedactorSpringAi redactor,
      @Qualifier("indexadorEjecutor") Executor ejecutor,
      @Value("${resolvo.ia.comprobar-al-arrancar:true}") boolean habilitada) {
    this.redactor = redactor;
    this.ejecutor = ejecutor;
    this.habilitada = habilitada;
  }

  @EventListener(ApplicationReadyEvent.class)
  void alArrancar() {
    if (habilitada) {
      ejecutor.execute(this::comprobar);
    }
  }

  void comprobar() {
    try {
      LOG.info("IA lista: el modelo de redaccion {} responde", redactor.comprobar());
    } catch (IaNoDisponible error) {
      LOG.warn("IA no responde al arrancar: {}", error.getMessage());
    }
  }
}
