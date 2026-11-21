package com.admaal.resolvo.infrastructure;

import com.admaal.resolvo.application.ComentariosDemo;
import com.admaal.resolvo.application.PurgarComentariosDemo;
import com.admaal.resolvo.application.Reloj;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Limpieza de la demo publica: cualquiera entra con un clic y lo que escribe lo ve el siguiente.
 * Solo existe con {@code resolvo.demo.purgar-comentarios=true}; corre al arrancar y cada 24 horas.
 */
@Component
@ConditionalOnProperty(name = "resolvo.demo.purgar-comentarios", havingValue = "true")
public class PurgaDeDemo {

  private static final Logger LOG = LoggerFactory.getLogger(PurgaDeDemo.class);

  private final PurgarComentariosDemo purga;

  public PurgaDeDemo(ComentariosDemo comentarios, Reloj reloj) {
    this.purga = new PurgarComentariosDemo(comentarios, reloj);
  }

  @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS)
  void purgar() {
    try {
      LOG.info("Demo: {} comentarios de visitantes borrados", purga.ejecutar());
    } catch (RuntimeException error) {
      LOG.warn("Demo: no se pudo limpiar los comentarios de visitantes", error);
    }
  }
}
