package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.ComentariosDemo;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ComentariosDemoJdbc implements ComentariosDemo {

  private final JdbcTemplate jdbc;

  public ComentariosDemoJdbc(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** La semilla (V3) queda marcada en V8 y nunca se borra. */
  @Override
  public int borrarCreadosAntesDe(Instant limite) {
    return jdbc.update(
        "delete from comentario where semilla = false and creado_en < ?", Timestamp.from(limite));
  }
}
