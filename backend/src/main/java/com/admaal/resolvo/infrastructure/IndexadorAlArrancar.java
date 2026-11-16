package com.admaal.resolvo.infrastructure;

import com.admaal.resolvo.application.IaNoDisponible;
import com.admaal.resolvo.domain.OrganizacionId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Indexa lo que falte al arrancar, en segundo plano y sin bloquear el arranque. */
@Component
public class IndexadorAlArrancar {

  private static final Logger LOG = LoggerFactory.getLogger(IndexadorAlArrancar.class);

  private final AplicacionDeConocimiento conocimiento;
  private final JdbcTemplate jdbc;
  private final Executor ejecutor;
  private final boolean habilitado;

  public IndexadorAlArrancar(
      AplicacionDeConocimiento conocimiento,
      JdbcTemplate jdbc,
      @Qualifier("indexadorEjecutor") Executor ejecutor,
      @Value("${resolvo.ia.indexar-al-arrancar:true}") boolean habilitado) {
    this.conocimiento = conocimiento;
    this.jdbc = jdbc;
    this.ejecutor = ejecutor;
    this.habilitado = habilitado;
  }

  @EventListener(ApplicationReadyEvent.class)
  void alArrancar() {
    if (habilitado) {
      ejecutor.execute(this::indexarTodo);
    }
  }

  void indexarTodo() {
    List<UUID> organizaciones = jdbc.queryForList("select id from organizacion", UUID.class);
    for (UUID id : organizaciones) {
      try {
        conocimiento.indexar().organizacion(new OrganizacionId(id));
      } catch (IaNoDisponible error) {
        LOG.info("Indexado omitido: {}", error.getMessage());
        return;
      } catch (RuntimeException error) {
        LOG.warn("No se pudo indexar la organizacion {}: {}", id, error.getMessage());
      }
    }
  }
}
