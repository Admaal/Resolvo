package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.RegistroLlamadas;
import com.admaal.resolvo.domain.Consumo;
import com.admaal.resolvo.domain.LlamadaIa;
import com.admaal.resolvo.domain.OrganizacionId;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RegistroLlamadasJdbc implements RegistroLlamadas {

  /** Tope de filas por consulta: el resumen se calcula en memoria y la demo gasta pocas llamadas. */
  static final int MAXIMO_DE_FILAS = 20_000;

  private final JdbcTemplate jdbc;

  public RegistroLlamadasJdbc(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void registrar(LlamadaIa llamada) {
    jdbc.update(
        """
        insert into llamada_ia
          (id, organizacion_id, operacion, modelo, tokens_entrada, tokens_salida, milisegundos, exito, ocurrida_en)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        llamada.id(),
        llamada.organizacionId() == null ? null : llamada.organizacionId().valor(),
        llamada.operacion(),
        llamada.modelo(),
        llamada.tokensEntrada(),
        llamada.tokensSalida(),
        llamada.milisegundos(),
        llamada.exito(),
        Timestamp.from(llamada.ocurridaEn()));
  }

  @Override
  public List<LlamadaIa> deLaOrganizacion(OrganizacionId organizacionId, Instant desde, Instant hasta) {
    return jdbc.query(
        """
        select id, organizacion_id, operacion, modelo, tokens_entrada, tokens_salida,
               milisegundos, exito, ocurrida_en
        from llamada_ia
        where organizacion_id = ? and ocurrida_en >= ? and ocurrida_en < ?
        order by ocurrida_en
        limit ?
        """,
        (fila, numero) ->
            new LlamadaIa(
                fila.getObject("id", UUID.class),
                new OrganizacionId(fila.getObject("organizacion_id", UUID.class)),
                fila.getString("operacion"),
                fila.getString("modelo"),
                fila.getInt("tokens_entrada"),
                fila.getInt("tokens_salida"),
                fila.getLong("milisegundos"),
                fila.getBoolean("exito"),
                fila.getTimestamp("ocurrida_en").toInstant()),
        organizacionId.valor(),
        Timestamp.from(desde),
        Timestamp.from(hasta),
        MAXIMO_DE_FILAS);
  }

  @Override
  public Consumo consumoDe(OrganizacionId organizacionId, Instant desde, Instant hasta) {
    return jdbc.queryForObject(
        """
        select count(*) as llamadas,
               coalesce(sum(tokens_entrada + tokens_salida), 0) as tokens
        from llamada_ia
        where organizacion_id = ? and ocurrida_en >= ? and ocurrida_en < ?
        """,
        (fila, numero) -> new Consumo(fila.getInt("llamadas"), fila.getLong("tokens")),
        organizacionId.valor(),
        Timestamp.from(desde),
        Timestamp.from(hasta));
  }
}
