package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.CodigosDeTicket;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CodigosDeTicketJdbc implements CodigosDeTicket {

  private final JdbcTemplate jdbc;

  public CodigosDeTicketJdbc(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public String siguiente(OrganizacionId organizacionId) {
    List<Integer> numeros =
        jdbc.query(
            """
            update secuencia_ticket
            set ultimo = ultimo + 1
            where organizacion_id = ?
            returning ultimo
            """,
            (rs, fila) -> rs.getInt("ultimo"),
            organizacionId.valor());
    if (numeros.isEmpty()) {
      throw new NoEncontrado();
    }
    return "RS-" + numeros.get(0);
  }
}
